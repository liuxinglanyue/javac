/*
 * @(#)Convert.java	1.25 07/03/21
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

package com.sun.tools.javac.util;

/** Utility class for static conversion methods between numbers
 *  and strings in various formats.
 *
 *  <p><b>This is NOT part of any API supported by Sun Microsystems.  If
 *  you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
@Version("@(#)Convert.java	1.25 07/03/21")
public class Convert {
	
	private static my.Debug DEBUG=new my.Debug(my.Debug.Convert);//�Ҽ��ϵ�
    /*ע��:
      java���Եġ���������ֵ(integer literal)��������10���ơ�8���ƻ���16����
    ��ʾ���������������ֵ��û�к�׺L(��l)�ַ����ʹ�����int���ͣ��������
    long���͡����⣬java����û���޷�����������int��long���ʹ��������������
    ��������int������32λ�����Ʊ�ʾһ���з������������λ�Ƿ���λ������λ
    ��0����ʾ��������(��0)������λ��1����ʾ�Ǹ�����(��0)��
      
      ����������ֵ��������׺L(��l)�����Σ�
      ����10���Ʊ�ʾһ������������ֵ��ʱ��ǰ׺��"+"��"-"�ʹ�����������������
    ����8���ƻ���16���Ʊ�ʾһ������������ֵ��ʱ���ÿ��ܵĶ�����λ�Ƿ�ﵽ��32
    λ(һ��8����λ��Ӧ3��������λ��һ��16����λ��Ӧ4��������λ)�����������λ
    ����<32����ô�������������ֵ������������(��0)�����������λ����=32����ô
    �����λ����1�ʹ�������(��0)����0�ʹ���������(��0)�����������λ����>32
    ȥ��ǰ�����е�0�󣬶�����λ��������>32������Ϊ�ˡ���������ֵ��������int��
    �����ܱ�ʾ�ķ�Χ��
      ֵ��ע�����:��8���ƻ���16���Ʊ�ʾ�ġ���������ֵ�����õ��ǲ�����ʽ�����
    ����������ֵ�������������������ô���������ľ���ֵ���ǡ���������ֵ���������
    ����������ֵ��������Ǹ���������ô�˸������ľ���ֵ���ǰѡ���������ֵ��������λ
    ����λȡ����Ȼ���1��
    
    ����: 
   ----------------------------------------------------------------------------
    16����
  ����������ֵ��               ������ֵ			                    10����ֵ	
   0x64                                           0110 0100(8 λ)   100
   0xFFFFFF9C       1111 1111 1111 1111 1111 1111 1001 1100(32λ)  -100
   0x00000000       0000 0000 0000 0000 0000 0000 0000 0000(32λ)   0
   0x00000001       0000 0000 0000 0000 0000 0000 0000 0001(32λ)   1 (��С������)
   0x7FFFFFFF       0111 1111 1111 1111 1111 1111 1111 1111(32λ)   2147483647 (���������)
   0x80000000       1000 0000 0000 0000 0000 0000 0000 0000(32λ)  -2147483648 (��С������)
   0xFFFFFFFF       1111 1111 1111 1111 1111 1111 1111 1111(32λ)  -1 (�������)
   0x0FFFFFFFF 0000 1111 1111 1111 1111 1111 1111 1111 1111(36λ)  -1 (�������)
   0x10FFFFFFF    1 0000 1111 1111 1111 1111 1111 1111 1111(33λ)   ���Ϸ�(���������)
   
     
   
   0xFFFFFF9C       1111 1111 1111 1111 1111 1111 1001 1100(32λ)  -100
   ����λȡ�� :     0000 0000 0000 0000 0000 0000 0110 0011(32λ)  
   0x00000001       0000 0000 0000 0000 0000 0000 0000 0001(32λ)   1
   ��1�þ���ֵ:     0000 0000 0000 0000 0000 0000 0110 0100(32λ)   100
   ----------------------------------------------------------------------------      
     
   ����������ֵ�����к�׺L(��l)���������������ƣ�ֻҪ��32�ĳ�64����int�ĳ�
    long�����ˡ�
    ����ο�<<Java Language Specification, Third Edition>> 3.10. Literals 

    */
    /** Convert string to integer.
     */
    public static int string2int(String s, int radix)
        throws NumberFormatException {
        try {//�Ҽ��ϵ�
		DEBUG.P(Convert.class,"string2int(String s, int radix)");
		DEBUG.P("radix="+radix+" s="+s);
		//ע��:���ڡ�int n = 0�������������,����ֵ0�ǵ���
        //���ֵ�,��ʱ����������8���Ƶ�0,Ҳ���ǻ���(radix)����8,������10
        
        if (radix == 10) { //10����
            return Integer.parseInt(s, radix);
        } else {
        	//��8���ƻ���16��������ֵ(Literal)ת����10���Ƶ��㷨��ο�
        	//com.sun.tools.javac.parser.Scanner��convertUnicode()�����е�ע��
            char[] cs = s.toCharArray();
            /*
            //8���ƻ���16��������ֵ���Ϊ037777777777��0xffffffff,
            //Integer.MAX_VALUE���Ա�ʾΪ017777777777��0x7fffffff,
            //��radixΪ8��16ʱlimit��ֵΪ003777777777��0x0fffffff,
            //����003777777777=017777777777>>2(Ҳ����Integer.MAX_VALUE / (8/2))
            //0x0fffffff=0x7fffffff>>3(Ҳ����Integer.MAX_VALUE / (16/2))
            
            ���ڹ�ʽn * radix + d����radix=8ʱ����limit=003777777777�滻n,��7�滻d:
            n * radix + d = 003777777777 * 8 + 7 = 003777777777 << 3 + 7 = 037777777777
            
            ���n>limit����n=(003777777777 + 1)�����빫ʽ:
            n * radix + d = (003777777777 + 1) * 8 + d
            ��d>=0(d������С��0,С��0ʱ�ǷǷ��ַ�)
                        ==> (003777777777 * 8 + 8 + d) > (003777777777 * 8 + 7 )
                        ==> (003777777777 * 8 + 8 + d) > 037777777777
                        ==> ������ʾ(���������)
                        
            ��radix=16ʱ��������           
            */
            int limit = Integer.MAX_VALUE / (radix/2);
            int n = 0;
            for (int i = 0; i < cs.length; i++) {
                int d = Character.digit(cs[i], radix);
                DEBUG.P("cs["+i+"]="+cs[i]+"  d="+d+"  limit="+limit+"  n1="+n+"  n2="+(n * radix + d)+"  Integer.MAX_VALUE - d="+(Integer.MAX_VALUE - d));
                if (n < 0 ||
                    n > limit ||
                    n * radix > Integer.MAX_VALUE - d)
                    throw new NumberFormatException();
                    /*ע��:
                    n * radix > Integer.MAX_VALUE - d
                    ��һ����Ҳ�Ǻܹؼ��ģ���8���ƻ���16��������ֵ(Literal)���
                    �ַ����Ϸ�ʱ(����8���Ƶ�����ֵ�������'9'����ַ�)����ִ��
                    ��d = Character.digit(cs[i], radix)��������d��ֵ�ͱ��
                    ��ֵ��Integer.MAX_VALUE - dҲ����ű�ɸ�ֵ���Ӷ�
                    ����n * radix > Integer.MAX_VALUE - d�Ľ��Ϊtrue�����׳��쳣
                    */
                n = n * radix + d;
            }
            return n;
        }
        
        }finally{//�Ҽ��ϵ�
		DEBUG.P(0,Convert.class,"string2int(String s, int radix)");
		}
    }

    /** Convert string to long integer.
     */
    public static long string2long(String s, int radix)
        throws NumberFormatException {
        if (radix == 10) {
            return Long.parseLong(s, radix);
        } else {
            char[] cs = s.toCharArray();
            long limit = Long.MAX_VALUE / (radix/2);
            long n = 0;
            for (int i = 0; i < cs.length; i++) {
                int d = Character.digit(cs[i], radix);
                if (n < 0 ||
                    n > limit ||
                    n * radix > Long.MAX_VALUE - d)
                    throw new NumberFormatException();
                n = n * radix + d;
            }
            return n;
        }
    }

/* Conversion routines between names, strings, and byte arrays in Utf8 format
 */

    /** Convert `len' bytes from utf8 to characters.
     *  Parameters are as in System.arraycopy
     *  Return first index in `dst' past the last copied char.
     *  @param src        The array holding the bytes to convert.
     *  @param sindex     The start index from which bytes are converted.
     *  @param dst        The array holding the converted characters..
     *  @param dindex     The start index from which converted characters
     *                    are written.
     *  @param len        The maximum number of bytes to convert.
     */
    public static int utf2chars(byte[] src, int sindex,
                                char[] dst, int dindex,
                                int len) {
        int i = sindex;
        int j = dindex;
        int limit = sindex + len;
        while (i < limit) {
            int b = src[i++] & 0xFF;
            if (b >= 0xE0) {
                b = (b & 0x0F) << 12;
                b = b | (src[i++] & 0x3F) << 6;
                b = b | (src[i++] & 0x3F);
            } else if (b >= 0xC0) {
                b = (b & 0x1F) << 6;
                b = b | (src[i++] & 0x3F);
            }
            dst[j++] = (char)b;
        }
        return j;
    }

    /** Return bytes in Utf8 representation as an array of characters.
     *  @param src        The array holding the bytes.
     *  @param sindex     The start index from which bytes are converted.
     *  @param len        The maximum number of bytes to convert.
     */
    public static char[] utf2chars(byte[] src, int sindex, int len) {
        char[] dst = new char[len];
        int len1 = utf2chars(src, sindex, dst, 0, len);
        char[] result = new char[len1];
        System.arraycopy(dst, 0, result, 0, len1);
        return result;
    }

    /** Return all bytes of a given array in Utf8 representation
     *  as an array of characters.
     *  @param src        The array holding the bytes.
     */
    public static char[] utf2chars(byte[] src) {
        return utf2chars(src, 0, src.length);
    }

    /** Return bytes in Utf8 representation as a string.
     *  @param src        The array holding the bytes.
     *  @param sindex     The start index from which bytes are converted.
     *  @param len        The maximum number of bytes to convert.
     */
    public static String utf2string(byte[] src, int sindex, int len) {
        char dst[] = new char[len];
        int len1 = utf2chars(src, sindex, dst, 0, len);
        return new String(dst, 0, len1);
    }

    /** Return all bytes of a given array in Utf8 representation
     *  as a string.
     *  @param src        The array holding the bytes.
     */
    public static String utf2string(byte[] src) {
        return utf2string(src, 0, src.length);
    }

    /** Copy characters in source array to bytes in target array,
     *  converting them to Utf8 representation.
     *  The target array must be large enough to hold the result.
     *  returns first index in `dst' past the last copied byte.
     *  @param src        The array holding the characters to convert.
     *  @param sindex     The start index from which characters are converted.
     *  @param dst        The array holding the converted characters..
     *  @param dindex     The start index from which converted bytes
     *                    are written.
     *  @param len        The maximum number of characters to convert.
     */
    public static int chars2utf(char[] src, int sindex,
                                byte[] dst, int dindex,
                                int len) {
        int j = dindex;
        int limit = sindex + len;
        for (int i = sindex; i < limit; i++) {
            char ch = src[i];
            if (1 <= ch && ch <= 0x7F) {
                dst[j++] = (byte)ch;
            } else if (ch <= 0x7FF) {
                dst[j++] = (byte)(0xC0 | (ch >> 6));
                dst[j++] = (byte)(0x80 | (ch & 0x3F));
            } else {
                dst[j++] = (byte)(0xE0 | (ch >> 12));
                dst[j++] = (byte)(0x80 | ((ch >> 6) & 0x3F));
                dst[j++] = (byte)(0x80 | (ch & 0x3F));
            }
        }
        return j;
    }

    /** Return characters as an array of bytes in Utf8 representation.
     *  @param src        The array holding the characters.
     *  @param sindex     The start index from which characters are converted.
     *  @param len        The maximum number of characters to convert.
     */
    public static byte[] chars2utf(char[] src, int sindex, int len) {
        byte[] dst = new byte[len * 3];
        int len1 = chars2utf(src, sindex, dst, 0, len);
        byte[] result = new byte[len1];
        System.arraycopy(dst, 0, result, 0, len1);
        return result;
    }

    /** Return all characters in given array as an array of bytes
     *  in Utf8 representation.
     *  @param src        The array holding the characters.
     */
    public static byte[] chars2utf(char[] src) {
        return chars2utf(src, 0, src.length);
    }

    /** Return string as an array of bytes in in Utf8 representation.
     */
    public static byte[] string2utf(String s) {
        return chars2utf(s.toCharArray());
    }

    /**
     * Escapes each character in a string that has an escape sequence or
     * is non-printable ASCII.  Leaves non-ASCII characters alone.
     */
    public static String quote(String s) {
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            buf.append(quote(s.charAt(i)));
        }
        return buf.toString();
    }

    /**
     * Escapes a character if it has an escape sequence or is
     * non-printable ASCII.  Leaves non-ASCII characters alone.
     */
    public static String quote(char ch) {
        switch (ch) {
        case '\b':  return "\\b";
        case '\f':  return "\\f";
        case '\n':  return "\\n";
        case '\r':  return "\\r";
        case '\t':  return "\\t";
        case '\'':  return "\\'";
        case '\"':  return "\\\"";
        case '\\':  return "\\\\";
        default:
            return (ch > 127 || isPrintableAscii(ch))
                ? String.valueOf(ch)
                : String.format("\\%03o", (int) ch);
        }
    }

    /**
     * Is a character printable ASCII?
     */
    private static boolean isPrintableAscii(char ch) {
        return ch >= ' ' && ch <= '~';
    }

    /** Escape all unicode characters in string.
     */
    public static String escapeUnicode(String s) {
        int len = s.length();
        int i = 0;
        while (i < len) {
            char ch = s.charAt(i);
            if (ch > 255) {
                StringBuffer buf = new StringBuffer();
                buf.append(s.substring(0, i));
                while (i < len) {
                    ch = s.charAt(i);
                    if (ch > 255) {
                        buf.append("\\u");
                        buf.append(Character.forDigit((ch >> 12) % 16, 16));
                        buf.append(Character.forDigit((ch >>  8) % 16, 16));
                        buf.append(Character.forDigit((ch >>  4) % 16, 16));
                        buf.append(Character.forDigit((ch      ) % 16, 16));
                    } else {
                        buf.append(ch);
                    }
                    i++;
                }
                s = buf.toString();
            } else {
                i++;
            }
        }
        return s;
    }

/* Conversion routines for qualified name splitting
 */
    /** Return the last part of a class name.
     */
    public static Name shortName(Name classname) {
        return classname.subName(
            classname.lastIndexOf((byte)'.') + 1, classname.len);
    }

    public static String shortName(String classname) {
        return classname.substring(classname.lastIndexOf('.') + 1);
    }

    /** Return the package name of a class name, excluding the trailing '.',
     *  "" if not existent.
     */
    public static Name packagePart(Name classname) {
        return classname.subName(0, classname.lastIndexOf((byte)'.'));
    }

    public static String packagePart(String classname) {
        int lastDot = classname.lastIndexOf('.');
        return (lastDot < 0 ? "" : classname.substring(0, lastDot));
    }

    public static List<Name> enclosingCandidates(Name name) {
        List<Name> names = List.nil();
        int index;
        while ((index = name.lastIndexOf((byte)'$')) > 0) {
            name = name.subName(0, index);
            names = names.prepend(name);
        }
        return names;
    }
}
