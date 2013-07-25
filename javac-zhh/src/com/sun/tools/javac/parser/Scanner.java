/*
 * @(#)Scanner.java	1.75 07/03/21
 * 
 * Copyright (c) 2007 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *  
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
 *  
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *  
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *  
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

package com.sun.tools.javac.parser;

import java.io.*;
import java.nio.*;
import java.nio.ByteBuffer;
import java.nio.charset.*;
import java.nio.channels.*;
import java.util.regex.*;

import com.sun.tools.javac.util.*;

import com.sun.tools.javac.code.Source;

import static com.sun.tools.javac.parser.Token.*;
import static com.sun.tools.javac.util.LayoutCharacters.*;

/** The lexical analyzer maps an input stream consisting of
 *  ASCII characters and Unicode escapes into a token sequence.
 *
 *  <p><b>This is NOT part of any API supported by Sun Microsystems.  If
 *  you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
@Version("@(#)Scanner.java	1.75 07/03/21")
public class Scanner implements Lexer {
	
	private static my.Debug DEBUG=new my.Debug(my.Debug.Scanner);//我加上的
	
	//源码中原来是false
    private static boolean scannerDebug = my.Debug.Scanner;
    
    //private static boolean scannerDebug = false;

    /** A factory for creating scanners. */
    public static class Factory {
	/** The context key for the scanner factory. */
	public static final Context.Key<Scanner.Factory> scannerFactoryKey =
	    new Context.Key<Scanner.Factory>();

	/** Get the Factory instance for this context. */
	public static Factory instance(Context context) {
	    Factory instance = context.get(scannerFactoryKey);
	    if (instance == null)
		instance = new Factory(context);
	    return instance;
	}

	final Log log;
	final Name.Table names;
	final Source source;
	final Keywords keywords;

	/** Create a new scanner factory. */
	protected Factory(Context context) {
		DEBUG.P(this,"Factory(1)");
	    context.put(scannerFactoryKey, this);
	    this.log = Log.instance(context);
	    this.names = Name.Table.instance(context);
	    this.source = Source.instance(context);
	    this.keywords = Keywords.instance(context);
	    DEBUG.P(0,this,"Factory(1)");
	}

        public Scanner newScanner(CharSequence input) {
        	try {//我加上的
        	DEBUG.P(this,"newScanner(1)");
        	//DEBUG.P("input instanceof CharBuffer="+(input instanceof CharBuffer));
        	/*
        	为什么要(input instanceof CharBuffer)呢？
        	因为每个要编译的源文件都被“包装”成一
        	个JavacFileManager.RegularFileObject类的实例 ,
        	RegularFileObject类实现了JavaFileObject接口,JavaFileObject接口的
        	超级接口是FileObject，在FileObject接口中有一个方法(用于读取文件内容):
        	java.lang.CharSequence getCharContent(boolean ignoreEncodingErrors)
                                      throws java.io.IOException
                                      
            而JavacFileManager.RegularFileObject类对应的实现方法为:
            public java.nio.CharBuffer getCharContent(boolean ignoreEncodingErrors)
                                   throws java.io.IOException
                                   
            比较两个方法的返回值，初看可能觉得有点怪，其实这是合法的，
            因为java.nio.CharBuffer类实现了java.lang.CharSequence接口                   
        	*/
            if (input instanceof CharBuffer) {
                return new Scanner(this, (CharBuffer)input);
            } else {
                char[] array = input.toString().toCharArray();
                return newScanner(array, array.length);
            }
            
            }finally{//我加上的
			DEBUG.P(0,this,"newScanner(1)");
			}
        }

        public Scanner newScanner(char[] input, int inputLength) {
            return new Scanner(this, input, inputLength);
        }
    }

    /* Output variables; set by nextToken():
     */

    /** The token, set by nextToken().
     */
    private Token token;

    /** Allow hex floating-point literals.
     */
    private boolean allowHexFloats;

    /** The token's position, 0-based offset from beginning of text.
     */
    private int pos;

    /** Character position just after the last character of the token.
     */
    private int endPos;

    /** The last character position of the previous token.
     */
    private int prevEndPos;
    
    /*举例说明:pos，endPos，prevEndPos这三者的区别
    例如要编译的源代码开头如下：
    package my.test;
    
    开启scannerDebug=true后会有如下输出:
    nextToken(0,7)=|package|  	tokenName=PACKAGE|  	prevEndPos=0
    processWhitespace(7,8)=| |
	nextToken(8,10)=|my|   		tokenName=IDENTIFIER|  	prevEndPos=7
	nextToken(10,11)=|.|  		tokenName=DOT|  		prevEndPos=10
	nextToken(11,15)=|test|  	tokenName=IDENTIFIER|  	prevEndPos=11
	nextToken(15,16)=|;|  		tokenName=SEMI|  		prevEndPos=15
	
	其中的(0,7)、(8,10)、(10,11)、(11,15)、(15,16)都是代表(pos,endPos)，
	endPos所代表的位置上的字符并不是当前Token的最后一个字符，而是下一
	个Token的起始字符或者空白、换行、注释文档符等。
	
	另外，prevEndPos总是指向前一个Token的endPos，prevEndPos并不指向
	空白、换行、注释文档的endPos，
	如processWhitespace(7,8)的endPos是8，但是此时prevEndPos=7
	*/


    /** The position where a lexical error occurred;
     */
    private int errPos = Position.NOPOS;

    /** The name of an identifier or token:
     */
    private Name name;

    /** The radix of a numeric literal token.
     */
    private int radix;

    /** Has a @deprecated been encountered in last doc comment?
     *  this needs to be reset by client.
     */
    protected boolean deprecatedFlag = false;

    /** A character buffer for literals.
     */
    private char[] sbuf = new char[128];//字符缓存，会经常变更
    private int sp;

    /** The input buffer, index of next chacter to be read,
     *  index of one past last character in buffer.
     */
    private char[] buf;//存放源方件内容
    private int bp;
    private int buflen;
    private int eofPos;

    /** The current character.
     */
    private char ch;

    /** The buffer index of the last converted unicode character
     */
    private int unicodeConversionBp = -1;

    /** The log to be used for error reporting.
     */
    private final Log log;

    /** The name table. */
    private final Name.Table names;

    /** The keyword table. */
    private final Keywords keywords;

    /** Common code for constructors. */
    private Scanner(Factory fac) {
	this.log = fac.log;
	this.names = fac.names;
	this.keywords = fac.keywords;
	//16进制浮点数只有>=JDK1.5才可以用
	this.allowHexFloats = fac.source.allowHexFloats();
    }

    private static final boolean hexFloatsWork = hexFloatsWork();
    private static boolean hexFloatsWork() {
        try {
            Float.valueOf("0x1.0p1");
            return true;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    /** Create a scanner from the input buffer.  buffer must implement
     *  array() and compact(), and remaining() must be less than limit().
     */
    protected Scanner(Factory fac, CharBuffer buffer) {
	this(fac, JavacFileManager.toArray(buffer), buffer.limit());
    }

    /**
     * Create a scanner from the input array.  This method might
     * modify the array.  To avoid copying the input array, ensure
     * that {@code inputLength < input.length} or
     * {@code input[input.length -1]} is a white space character.
     * 
     * @param fac the factory which created this Scanner
     * @param input the input, might be modified
     * @param inputLength the size of the input.
     * Must be positive and less than or equal to input.length.
     */
    protected Scanner(Factory fac, char[] input, int inputLength) {
		this(fac);
		
		DEBUG.P(this,"Scanner(3) 源文件内容预览......");
		//input字符数组中存放的内容与源文件不完全一样，比源文件多了10个
		//null字符(10进制等于0),是在源文件后面加上的,
		//inputLength是源文件内容的长度,input.length一般等于inputLength+10
    	//DEBUG.P(new String(input)+"");
    	//DEBUG.P("---------------------------");
    	DEBUG.P("buffer.limit="+inputLength);
    	DEBUG.P("input.length="+input.length);
    	
	    eofPos = inputLength;
	    //这种情况只是为了方便在buf最后放入EOI而进行的特殊处理
		if (inputLength == input.length) {
				//查看java.lang.Character的isWhitespace()方法,null字符会返回false
	            if (input.length > 0 && Character.isWhitespace(input[input.length - 1])) {
	                inputLength--;
	            } else {
	                char[] newInput = new char[inputLength + 1];
	                System.arraycopy(input, 0, newInput, 0, input.length);
	                input = newInput;
	            }
	    }
		buf = input;
		buflen = inputLength;
		buf[buflen] = EOI;//EOI在com.sun.tools.javac.util.LayoutCharacters定义
	    bp = -1;
		scanChar();
	
		DEBUG.P("scan first char="+ch);
		DEBUG.P(0,this,"Scanner(3)");
    }

    /** Report an error at the given position using the provided arguments.
     */
    private void lexError(int pos, String key, Object... args) {
        log.error(pos, key, args);
        token = ERROR;
        errPos = pos;
    }

    /** Report an error at the current token position using the provided
     *  arguments.
     */
    private void lexError(String key, Object... args) {
	lexError(pos, key, args);
    }

    /** Convert an ASCII digit from its base (8, 10, or 16)
     *  to its value.
     */
    private int digit(int base) {//如16进制的A会转换成10
	char c = ch;
	int result = Character.digit(c, base);
	if (result >= 0 && c > 0x7f) {
		//非法的非 ASCII 数字
	    lexError(pos+1, "illegal.nonascii.digit");
	    ch = "0123456789abcdef".charAt(result);
	}
	return result;
    }

    /** Convert unicode escape; bp points to initial '\' character
     *  (Spec 3.3).
     */
    private void convertUnicode() {
	if (ch == '\\' && unicodeConversionBp != bp) {
	    bp++; ch = buf[bp];
	   	/*
	    (注:注释里的\\u必须有两个\，如果只有一个\，会有编译错误(也许是javac的BUG),参考scanCommentChar())
	    unicode字符只能是以\\u开头,不能以\\U(大写的U)开头
	    */
	    if (ch == 'u') {//在\后面可以接不只一个u
		do {
		    bp++; ch = buf[bp];
		} while (ch == 'u');
		int limit = bp + 3;//每一个unicode占4个16进制字符
		if (limit < buflen) {
		    int d = digit(16);
		    int code = d;
		    while (bp < limit && d >= 0) {
			bp++; ch = buf[bp];
			d = digit(16);
			code = (code << 4) + d;
			//从高位到低位依次计算10进制值,
			//因为一个16进制字符用4个二进制字符表示，所以每次左移4位，
			//相当于10进制值每次乘以16
			/*
			举例:
			unicod码:   \uA971
			10进制码:   10*16*16*16 + 9*16*16 + 7*16 + 1
			            =(10*16 + 9)*16*16 + 7*16 + 1
			            =((10*16 + 9)*16 + 7)*16 + 1
			            =((10<<4 + 9)<<4 + 7)<<4 + 1
			            
			正好对应公式:(code << 4) + d;
			*/
		    }
		    if (d >= 0) {
			ch = (char)code;
			unicodeConversionBp = bp;
			return;
		    }
		}
			//非法的 Unicode 转义,如下面的g是非法的
			//public int myInt='\\uuuuugfff';
            //                         ^  
		lexError(bp, "illegal.unicode.esc");
	    } else {
	    //如果'\'字符后面不是'u'，说明不是Unicode，往后退一位
		bp--;
		ch = '\\';
	    }
	}
    }

    /** Read next character.
     */
    private void scanChar() {
	ch = buf[++bp];
	if (ch == '\\') {
	    convertUnicode();
	}
    }

    /** Read next character in comment, skipping over double '\' characters.
     */
    private void scanCommentChar() {
	scanChar();
	if (ch == '\\') {
	    if (buf[bp+1] == '\\' && unicodeConversionBp != bp) {
		bp++;
	    } else {
		convertUnicode();
	    }
	}
    }

    /** Append a character to sbuf.
     */
    private void putChar(char ch) {
	if (sp == sbuf.length) {
	    char[] newsbuf = new char[sbuf.length * 2];
	    System.arraycopy(sbuf, 0, newsbuf, 0, sbuf.length);
	    sbuf = newsbuf;
	}
	sbuf[sp++] = ch;
    }

    /** For debugging purposes: print character.
     */
    private void dch() {
        System.err.print(ch); System.out.flush();
    }

    /** Read next character in character or string literal and copy into sbuf.
     */
    private void scanLitChar() {
        if (ch == '\\') {
	    if (buf[bp+1] == '\\' && unicodeConversionBp != bp) {
		bp++;
		putChar('\\');
		scanChar();
	    } else {
		scanChar();
		switch (ch) {
		case '0': case '1': case '2': case '3':
		case '4': case '5': case '6': case '7':
		    char leadch = ch;
		    int oct = digit(8);
		    scanChar();
		    if ('0' <= ch && ch <= '7') {
			oct = oct * 8 + digit(8);
			scanChar();
			//用\表示8进制的字符时，当8进制的字符占3位时，为何第一位leadch <= '3' ????
			if (leadch <= '3' && '0' <= ch && ch <= '7') {
			    oct = oct * 8 + digit(8);
			    scanChar();
			}
		    }
		    putChar((char)oct);
		    break;
		case 'b':
		    putChar('\b'); scanChar(); break;
		case 't':
		    putChar('\t'); scanChar(); break;
		case 'n':
		    putChar('\n'); scanChar(); break;
		case 'f':
		    putChar('\f'); scanChar(); break;
		case 'r':
		    putChar('\r'); scanChar(); break;
		case '\'':
		    putChar('\''); scanChar(); break;
		case '\"':
		    putChar('\"'); scanChar(); break;
		case '\\':
		    putChar('\\'); scanChar(); break;
		default:
 		    lexError(bp, "illegal.esc.char");
		}
	    }
	} else if (bp != buflen) {
            putChar(ch); scanChar();
        }
    }

    /** Read fractional part of hexadecimal floating point number.
     */
    private void scanHexExponentAndSuffix() {
    	//16进制浮点指数部分(注:p(或P)后面是指数,不能省略,如果是float类型则f(或F)也是必须的)
        if (ch == 'p' || ch == 'P') {
	    putChar(ch);
            scanChar();
            if (ch == '+' || ch == '-') {
		putChar(ch);
                scanChar();
	    }
	    if ('0' <= ch && ch <= '9') {
		do {
		    putChar(ch);
		    scanChar();
		} while ('0' <= ch && ch <= '9');
		if (!allowHexFloats) {
		    lexError("unsupported.fp.lit");
                    allowHexFloats = true;
                }
                else if (!hexFloatsWork)
		    lexError("unsupported.cross.fp.lit");
	    } else
		lexError("malformed.fp.lit");
	} else {
	    lexError("malformed.fp.lit");
	}
	if (ch == 'f' || ch == 'F') {
	    putChar(ch);
	    scanChar();
            token = FLOATLITERAL;
	} else {
	    if (ch == 'd' || ch == 'D') {
		putChar(ch);
		scanChar();
	    }
	    token = DOUBLELITERAL;
	}
    }

    /** Read fractional part of floating point number.
     */
    private void scanFraction() {
        while (digit(10) >= 0) {
	    putChar(ch);
            scanChar();
        }
	int sp1 = sp;
        if (ch == 'e' || ch == 'E') {
	    putChar(ch);
            scanChar();
            if (ch == '+' || ch == '-') {
		putChar(ch);
                scanChar();
	    }
	    if ('0' <= ch && ch <= '9') {
		do {
		    putChar(ch);
		    scanChar();
		} while ('0' <= ch && ch <= '9');
		return;
	    }
	    lexError("malformed.fp.lit");
	    sp = sp1;
	}
    }

    /** Read fractional part and 'd' or 'f' suffix of floating point number.
     */
    private void scanFractionAndSuffix() {
	this.radix = 10;
	scanFraction();
	if (ch == 'f' || ch == 'F') {
	    putChar(ch);
	    scanChar();
            token = FLOATLITERAL;
	} else {
	    if (ch == 'd' || ch == 'D') {
		putChar(ch);
		scanChar();
	    }
	    token = DOUBLELITERAL;
	}
    }

    /** Read fractional part and 'd' or 'f' suffix of floating point number.
     */
    private void scanHexFractionAndSuffix(boolean seendigit) {
	this.radix = 16;
	assert ch == '.';
	putChar(ch);
	scanChar();
        while (digit(16) >= 0) {
	    seendigit = true;
	    putChar(ch);
            scanChar();
        }
	if (!seendigit)
	    lexError("invalid.hex.number");//十六进制数字必须包含至少一位十六进制数,错例如:0x.p-1f;
	else
	    scanHexExponentAndSuffix();
    }

    /** Read a number.
     *  @param radix  The radix of the number; one of 8, 10, 16.
     */
    private void scanNumber(int radix) {
	this.radix = radix;
	// for octal, allow base-10 digit in case it's a float literal
	int digitRadix = (radix <= 10) ? 10 : 16;
	boolean seendigit = false;
	while (digit(digitRadix) >= 0) {
	    seendigit = true;
	    putChar(ch);
	    scanChar();
	}
	if (radix == 16 && ch == '.') {
	    scanHexFractionAndSuffix(seendigit);
	} else if (seendigit && radix == 16 && (ch == 'p' || ch == 'P')) {
		//如:0x1p-1f的情况
	    scanHexExponentAndSuffix();
	} else if (radix <= 10 && ch == '.') {
	    putChar(ch);
	    scanChar();
	    scanFractionAndSuffix();
	} else if (radix <= 10 &&
		   (ch == 'e' || ch == 'E' ||
		    ch == 'f' || ch == 'F' ||
		    ch == 'd' || ch == 'D')) {
	    scanFractionAndSuffix();
	} else {
	    if (ch == 'l' || ch == 'L') {
		scanChar();
		token = LONGLITERAL;
	    } else {
		token = INTLITERAL;
	    }
	}
    }

    /** Read an identifier.
     */
    private void scanIdent() {
	boolean isJavaIdentifierPart;
	char high;
	do {
	    if (sp == sbuf.length) putChar(ch); else sbuf[sp++] = ch;
	    // optimization, was: putChar(ch);

	    scanChar();
	    switch (ch) {
	    case 'A': case 'B': case 'C': case 'D': case 'E':
	    case 'F': case 'G': case 'H': case 'I': case 'J':
	    case 'K': case 'L': case 'M': case 'N': case 'O':
	    case 'P': case 'Q': case 'R': case 'S': case 'T':
	    case 'U': case 'V': case 'W': case 'X': case 'Y':
	    case 'Z':
	    case 'a': case 'b': case 'c': case 'd': case 'e':
	    case 'f': case 'g': case 'h': case 'i': case 'j':
	    case 'k': case 'l': case 'm': case 'n': case 'o':
	    case 'p': case 'q': case 'r': case 's': case 't':
	    case 'u': case 'v': case 'w': case 'x': case 'y':
	    case 'z':
	    case '$': case '_':
	    case '0': case '1': case '2': case '3': case '4':
	    case '5': case '6': case '7': case '8': case '9':
            case '\u0000': case '\u0001': case '\u0002': case '\u0003':
            case '\u0004': case '\u0005': case '\u0006': case '\u0007':
            case '\u0008': case '\u000E': case '\u000F': case '\u0010':
            case '\u0011': case '\u0012': case '\u0013': case '\u0014':
            case '\u0015': case '\u0016': case '\u0017':
            case '\u0018': case '\u0019': case '\u001B':
            case '\u007F':
		break;
            case '\u001A': // EOI is also a legal identifier part
                if (bp >= buflen) {
                    name = names.fromChars(sbuf, 0, sp);
                    token = keywords.key(name);
                    return;
                }
                break;
	    default:
                if (ch < '\u0080') {
                    // all ASCII range chars already handled, above
                    isJavaIdentifierPart = false;
                } else {//处理例如中文变量的情况
		    high = scanSurrogates();
                    if (high != 0) {
	                if (sp == sbuf.length) {
                            putChar(high);
                        } else {
                            sbuf[sp++] = high;
                        }
                        isJavaIdentifierPart = Character.isJavaIdentifierPart(
                            Character.toCodePoint(high, ch));
                    } else {
                        isJavaIdentifierPart = Character.isJavaIdentifierPart(ch);
                    }
                }
        //如果isJavaIdentifierPart为false，代表标识符识别结束
		if (!isJavaIdentifierPart) {
			//标识符识别后会存入name表中
		    name = names.fromChars(sbuf, 0, sp);
		    token = keywords.key(name);
		    return;
		}
	    }
	} while (true);
    }

    /** Are surrogates supported?
     */
    final static boolean surrogatesSupported = surrogatesSupported();
    private static boolean surrogatesSupported() {
        try {
            Character.isHighSurrogate('a');
            return true;
        } catch (NoSuchMethodError ex) {
            return false;
        }
    }

    /** Scan surrogate pairs.  If 'ch' is a high surrogate and
     *  the next character is a low surrogate, then put the low
     *  surrogate in 'ch', and return the high surrogate.
     *  otherwise, just return 0.
     */
    private char scanSurrogates() {
        if (surrogatesSupported && Character.isHighSurrogate(ch)) {
            char high = ch;

            scanChar();

            if (Character.isLowSurrogate(ch)) {
                return high;
            }

            ch = high;
        }

        return 0;
    }

    /** Return true if ch can be part of an operator.
     */
    private boolean isSpecial(char ch) {
        switch (ch) {
        case '!': case '%': case '&': case '*': case '?':
        case '+': case '-': case ':': case '<': case '=':
        case '>': case '^': case '|': case '~':
	case '@':
            return true;
        default:
            return false;
        }
    }

    /** Read longest possible sequence of special characters and convert
     *  to token.
     */
    private void scanOperator() {
	while (true) {
	    putChar(ch);
	    Name newname = names.fromChars(sbuf, 0, sp);
            if (keywords.key(newname) == IDENTIFIER) {
		sp--;
		break;
	    }
            name = newname;
            token = keywords.key(newname);
	    scanChar();
	    if (!isSpecial(ch)) break;
	}
    }

    /**
     * Scan a documention comment; determine if a deprecated tag is present.
     * Called once the initial /, * have been skipped, positioned at the second *
     * (which is treated as the beginning of the first line).
     * Stops positioned at the closing '/'.
     */
    @SuppressWarnings("fallthrough")
    private void scanDocComment() {
	boolean deprecatedPrefix = false;

	forEachLine:
	while (bp < buflen) {

	    // Skip optional WhiteSpace at beginning of line
	    while (bp < buflen && (ch == ' ' || ch == '\t' || ch == FF)) {
		scanCommentChar();
	    }

	    // Skip optional consecutive Stars
	    while (bp < buflen && ch == '*') {
		scanCommentChar();
		if (ch == '/') {
		    return;
		}
	    }
	
	    // Skip optional WhiteSpace after Stars
	    while (bp < buflen && (ch == ' ' || ch == '\t' || ch == FF)) {
		scanCommentChar();
	    }

	    deprecatedPrefix = false;
	    // At beginning of line in the JavaDoc sense.
	    if (bp < buflen && ch == '@' && !deprecatedFlag) {
		scanCommentChar();
		if (bp < buflen && ch == 'd') {
		    scanCommentChar();
		    if (bp < buflen && ch == 'e') {
			scanCommentChar();
			if (bp < buflen && ch == 'p') {
			    scanCommentChar();
			    if (bp < buflen && ch == 'r') {
				scanCommentChar();
				if (bp < buflen && ch == 'e') {
				    scanCommentChar();
				    if (bp < buflen && ch == 'c') {
					scanCommentChar();
					if (bp < buflen && ch == 'a') {
					    scanCommentChar();
					    if (bp < buflen && ch == 't') {
						scanCommentChar();
						if (bp < buflen && ch == 'e') {
						    scanCommentChar();
						    if (bp < buflen && ch == 'd') {
							deprecatedPrefix = true;
							scanCommentChar();
						    }}}}}}}}}}}
	    if (deprecatedPrefix && bp < buflen) {
		if (Character.isWhitespace(ch)) {
		    deprecatedFlag = true;
		} else if (ch == '*') {
		    scanCommentChar();
		    if (ch == '/') {
			deprecatedFlag = true;
			return;
		    }
		}
	    }

	    // Skip rest of line
	    while (bp < buflen) {
		switch (ch) {
		case '*':
		    scanCommentChar();
		    if (ch == '/') {
			return;
		    }
		    break;
		case CR: // (Spec 3.4)
		    scanCommentChar();
		    if (ch != LF) {
			continue forEachLine;
		    }
		    /* fall through to LF case */
		case LF: // (Spec 3.4)
		    scanCommentChar();
		    continue forEachLine;
		default:
		    scanCommentChar();
		}
	    } // rest of line
	} // forEachLine
	return;
    }

    /** The value of a literal token, recorded as a string.
     *  For integers, leading 0x and 'l' suffixes are suppressed.
     */
    public String stringVal() {
	return new String(sbuf, 0, sp);
    }

    /** Read token.
     */
    public void nextToken() {

	try {
	    prevEndPos = endPos;
	    sp = 0;
	
	    while (true) {
	    //处理完processWhiteSpace()与processLineTerminator()两个
	    //方法后，继续往下扫描字符
		pos = bp;
		switch (ch) {
		case ' ': // (Spec 3.6)
		case '\t': // (Spec 3.6)
		case FF: // (Spec 3.6)   //form feed是指换页
		    do {
			scanChar();
		    } while (ch == ' ' || ch == '\t' || ch == FF);
		    endPos = bp;
		    processWhiteSpace();
		    break;
		case LF: // (Spec 3.4)   //换行,有的系统生成的文件可能没有回车符
		    scanChar();
		    endPos = bp;
		    processLineTerminator();
		    break;
		case CR: // (Spec 3.4)   //回车,回车符后面跟换行符
		    scanChar();
		    if (ch == LF) {
			scanChar();
		    }
		    endPos = bp;
		    processLineTerminator();
		    break;
		//符合java标识符(或保留字)的首字母的情况之一
		case 'A': case 'B': case 'C': case 'D': case 'E':
		case 'F': case 'G': case 'H': case 'I': case 'J':
		case 'K': case 'L': case 'M': case 'N': case 'O':
		case 'P': case 'Q': case 'R': case 'S': case 'T':
		case 'U': case 'V': case 'W': case 'X': case 'Y':
		case 'Z':
		case 'a': case 'b': case 'c': case 'd': case 'e':
		case 'f': case 'g': case 'h': case 'i': case 'j':
		case 'k': case 'l': case 'm': case 'n': case 'o':
		case 'p': case 'q': case 'r': case 's': case 't':
		case 'u': case 'v': case 'w': case 'x': case 'y':
		case 'z':
		case '$': case '_':
		    scanIdent();
		    return;
		case '0': //16或8进制数的情况
		    scanChar();
		    if (ch == 'x' || ch == 'X') {
			scanChar();
			if (ch == '.') {
				//参数为false表示在小数点之前没有数字
			    scanHexFractionAndSuffix(false);
			} else if (digit(16) < 0) {
			    lexError("invalid.hex.number");
			} else {
			    scanNumber(16);
			}
		    } else {
			putChar('0');
			scanNumber(8);
		    }
		    return;
		case '1': case '2': case '3': case '4':
		case '5': case '6': case '7': case '8': case '9':
		    scanNumber(10);
		    return;
		case '.':
		    scanChar();
		    if ('0' <= ch && ch <= '9') {
			putChar('.');
			scanFractionAndSuffix();
		    } else if (ch == '.') {  //检测是否是省略符号(...)
			putChar('.'); putChar('.');
			scanChar();
			if (ch == '.') {
			    scanChar();
			    putChar('.');
			    token = ELLIPSIS;
			} else {  //否则认为是浮点错误
			    lexError("malformed.fp.lit");
			}
		    } else {
			token = DOT;
		    }
		    return;
		case ',':
		    scanChar(); token = COMMA; return;
		case ';':
		    scanChar(); token = SEMI; return;
		case '(':
		    scanChar(); token = LPAREN; return;
		case ')':
		    scanChar(); token = RPAREN; return;
		case '[':
		    scanChar(); token = LBRACKET; return;
		case ']':
		    scanChar(); token = RBRACKET; return;
		case '{':
		    scanChar(); token = LBRACE; return;
		case '}':
		    scanChar(); token = RBRACE; return;
		case '/':
		    scanChar();
		    if (ch == '/') {
			do {
			    scanCommentChar();
			} while (ch != CR && ch != LF && bp < buflen);
			if (bp < buflen) {
			    endPos = bp;
			    processComment(CommentStyle.LINE);
			}
			break;
		    } else if (ch == '*') {
			scanChar();
                        CommentStyle style;
			if (ch == '*') {
                            style = CommentStyle.JAVADOC;
			    scanDocComment();
			} else {
                            style = CommentStyle.BLOCK;
			    while (bp < buflen) {
				if (ch == '*') {
				    scanChar();
				    if (ch == '/') break;
				} else {
				    scanCommentChar();
				}
			    }
			}
			if (ch == '/') {
			    scanChar();
			    endPos = bp;
			    processComment(style);
			    break;
			} else {
			    lexError("unclosed.comment");
			    return;
			}
		    } else if (ch == '=') {
			name = names.slashequals;
			token = SLASHEQ;
			scanChar();
		    } else {
			name = names.slash;
			token = SLASH;
		    }
		    return;
		case '\'':  //字符与字符串都不能跨行
		    scanChar();
		    if (ch == '\'') {
			lexError("empty.char.lit");  //空字符字面值
		    } else {
			if (ch == CR || ch == LF)
			    lexError(pos, "illegal.line.end.in.char.lit");//字符字面值的行结尾不合法
			scanLitChar();
			if (ch == '\'') {
			    scanChar();
			    token = CHARLITERAL;
			} else {
			    lexError(pos, "unclosed.char.lit");
			}
		    }
		    return;
		case '\"':
		    scanChar();
		    while (ch != '\"' && ch != CR && ch != LF && bp < buflen)
			scanLitChar();
		    if (ch == '\"') {
			token = STRINGLITERAL;
			scanChar();
		    } else {
			lexError(pos, "unclosed.str.lit");
		    }
		    return;
		default:
		    if (isSpecial(ch)) { //可以作为操作符的某一部分的字符
			scanOperator();
		    } else {
		    	//这里处理其它字符,如中文变量之类的
		    	//与scanIdent()有相同的部分
		    	//注意这里是Start，而scanIdent()是Part
                        boolean isJavaIdentifierStart;
                        if (ch < '\u0080') {
                            // all ASCII range chars already handled, above
                            isJavaIdentifierStart = false;
                        } else {
                            char high = scanSurrogates();
                            if (high != 0) {
	                        if (sp == sbuf.length) {
                                    putChar(high);
                                } else {
                                    sbuf[sp++] = high;
                                }

                                isJavaIdentifierStart = Character.isJavaIdentifierStart(
                                    Character.toCodePoint(high, ch));
                            } else {
                                isJavaIdentifierStart = Character.isJavaIdentifierStart(ch);
                            }
                        }
                        if (isJavaIdentifierStart) {
			    scanIdent();
		        } else if (bp == buflen || ch == EOI && bp+1 == buflen) { // JLS 3.5
			    token = EOF;
                            pos = bp = eofPos;
		        } else {
                            lexError("illegal.char", String.valueOf((int)ch));
			    scanChar();
		        }
		    }
		    return;
		}
	    }
	} finally {
	    endPos = bp;
	    /*
	    if (scannerDebug)
		System.out.println("nextToken(" + pos
				   + "," + endPos + ")=|" +
				   new String(getRawCharacters(pos, endPos))
				   + "|");
		*/
		
		//我多加了tokenName=...(方便查看调试结果)
		if (scannerDebug)
		System.out.println("nextToken(" + pos
				   + "," + endPos + ")=|" +
				   new String(getRawCharacters(pos, endPos))
				   + "|  tokenName=|"+token+ "|  prevEndPos="+prevEndPos);
	}
    }

    /** Return the current token, set by nextToken().
     */
    public Token token() {
        return token;
    }

    /** Sets the current token.
     */
    public void token(Token token) {
        this.token = token;
    }

    /** Return the current token's position: a 0-based
     *  offset from beginning of the raw input stream
     *  (before unicode translation)
     */
    public int pos() {
        return pos;
    }

    /** Return the last character position of the current token.
     */
    public int endPos() {
        return endPos;
    }

    /** Return the last character position of the previous token.
     */
    public int prevEndPos() {
        return prevEndPos;
    }

    /** Return the position where a lexical error occurred;
     */
    public int errPos() {
        return errPos;
    }

    /** Set the position where a lexical error occurred;
     */
    public void errPos(int pos) {
        errPos = pos;
    }

    /** Return the name of an identifier or token for the current token.
     */
    public Name name() {
        return name;
    }

    /** Return the radix of a numeric literal token.
     */
    public int radix() {
        return radix;
    }

    /** Has a @deprecated been encountered in last doc comment?
     *  This needs to be reset by client with resetDeprecatedFlag.
     */
    public boolean deprecatedFlag() {
        return deprecatedFlag;
    }

    public void resetDeprecatedFlag() {
        deprecatedFlag = false;
    }

    /**
     * Returns the documentation string of the current token.
     */
    public String docComment() {
        return null;
    }

    /**
     * Returns a copy of the input buffer, up to its inputLength.
     * Unicode escape sequences are not translated.
     */
    public char[] getRawCharacters() {  //此方法暂时没什么用处,只为了实现Lexer接口而加的
        char[] chars = new char[buflen];
        System.arraycopy(buf, 0, chars, 0, buflen);
        return chars;
    }

    /**
     * Returns a copy of a character array subset of the input buffer.
     * The returned array begins at the <code>beginIndex</code> and
     * extends to the character at index <code>endIndex - 1</code>.
     * Thus the length of the substring is <code>endIndex-beginIndex</code>.
     * This behavior is like 
     * <code>String.substring(beginIndex, endIndex)</code>.
     * Unicode escape sequences are not translated.
     *
     * @param beginIndex the beginning index, inclusive.
     * @param endIndex the ending index, exclusive.
     * @throws IndexOutOfBounds if either offset is outside of the
     *         array bounds
     */
    public char[] getRawCharacters(int beginIndex, int endIndex) {
    	//length不是关键字,可以当变量名
    	//endIndex就是endPos,buf[endPos]的字符不会输出，会作为下次扫描的起点
        int length = endIndex - beginIndex;
        char[] chars = new char[length];
        System.arraycopy(buf, beginIndex, chars, 0, length);
        return chars;
    }

    public enum CommentStyle {
        LINE,
        BLOCK,
        JAVADOC,
    }

    /**
     * Called when a complete comment has been scanned. pos and endPos 
     * will mark the comment boundary.
     */
    protected void processComment(CommentStyle style) {
	if (scannerDebug)
	    System.out.println("processComment(" + pos
			       + "," + endPos + "," + style + ")=|"
                               + new String(getRawCharacters(pos, endPos))
			       + "|");
    }

    /**
     * Called when a complete whitespace run has been scanned. pos and endPos 
     * will mark the whitespace boundary.
     */
    protected void processWhiteSpace() {
	if (scannerDebug)
	    System.out.println("processWhitespace(" + pos
			       + "," + endPos + ")=|" +
			       new String(getRawCharacters(pos, endPos))
			       + "|");
    }

    /**
     * Called when a line terminator has been processed.
     */
    protected void processLineTerminator() {
	if (scannerDebug)
	    System.out.println("processTerminator(" + pos
			       + "," + endPos + ")=|" +
			       new String(getRawCharacters(pos, endPos))
			       + "|");
    }

    /** Build a map for translating between line numbers and
     * positions in the input.
     *
     * @return a LineMap */
    public Position.LineMap getLineMap() {
	return Position.makeLineMap(buf, buflen, false);
    }

}

