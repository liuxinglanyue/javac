/*
 * @(#)OptionName.java	1.4 07/03/21
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

package com.sun.tools.javac.main;

import com.sun.tools.javac.util.Version;

/**
 * TODO: describe com.sun.tools.javac.main.OptionName
 *
 * <p><b>This is NOT part of any API supported by Sun Microsystems.
 * If you write code that depends on this, you do so at your own
 * risk.  This code and its internal interfaces are subject to change
 * or deletion without notice.</b></p>
 */
@Version("@(#)OptionName.java	1.4 07/03/21")
public enum OptionName {
/*
�÷���javac <ѡ��> <Դ�ļ�>
���У����ܵ�ѡ�������
 -g                           �������е�����Ϣ
 -g:none                      �������κε�����Ϣ
 -g:{lines,vars,source}       ֻ����ĳЩ������Ϣ
 -nowarn                      �������κξ���
 -verbose                     ����йر���������ִ�еĲ�������Ϣ
 -deprecation                 ���ʹ���ѹ�ʱ�� API ��Դλ��
 -classpath <·��>            ָ�������û����ļ���ע�ʹ�������λ��
 -cp <·��>                   ָ�������û����ļ���ע�ʹ�������λ��
 -sourcepath <·��>           ָ����������Դ�ļ���λ��
 -bootclasspath <·��>        �����������ļ���λ��
 -extdirs <Ŀ¼>              ���ǰ�װ����չĿ¼��λ��
 -endorseddirs <Ŀ¼>         ����ǩ���ı�׼·����λ��
 -proc:{none, only}           �����Ƿ�ִ��ע�ʹ����/����롣
 -processor <class1>[,<class2>,<class3>...]Ҫ���е�ע�ʹ����������ƣ��ƹ�Ĭ�ϵ���������
 -processorpath <·��>        ָ������ע�ʹ�������λ��
 -d <Ŀ¼>                    ָ��������ɵ����ļ���λ��
 -s <Ŀ¼>                    ָ��������ɵ�Դ�ļ���λ��
 -encoding <����>             ָ��Դ�ļ�ʹ�õ��ַ�����
 -source <�汾>               �ṩ��ָ���汾��Դ������
 -target <�汾>               �����ض� VM �汾�����ļ�
 -version                     �汾��Ϣ
 -help                        �����׼ѡ�����Ҫ
 -Akey[=value]                ���ݸ�ע�ʹ�������ѡ��
 -X                           ����Ǳ�׼ѡ�����Ҫ
 -J<��־>                     ֱ�ӽ� <��־> ���ݸ�����ʱϵͳ


 -Xlint                       ���ý���ľ���
 -Xlint:{all,cast,deprecation,divzero,empty,unchecked,fallthrough,path,serial,f
         nally,overrides,-cast,-deprecation,-divzero,-empty,-unchecked,-fallthrough,-pat
         ,-serial,-finally,-overrides,none}���û�����ض��ľ���
 -Xbootclasspath/p:<·��>     ����������·��֮ǰ
 -Xbootclasspath/a:<·��>     ����������·��֮��
 -Xbootclasspath:<·��>       �����������ļ���λ��
 -Djava.ext.dirs=<Ŀ¼>       ���ǰ�װ����չĿ¼��λ��
 -Djava.endorsed.dirs=<Ŀ¼>  ����ǩ���ı�׼·����λ��
 -Xmaxerrs <���>             ����Ҫ����Ĵ���������Ŀ
 -Xmaxwarns <���>            ����Ҫ����ľ���������Ŀ
 -Xstdout <�ļ���>            �ض����׼���
 -Xprint                      ���ָ�����͵��ı���ʾ
 -XprintRounds                ����й�ע�ʹ���ѭ������Ϣ
 -XprintProcessorInfo         ����й��������������Щע�͵���Ϣ

��Щѡ��ǷǱ�׼ѡ����и��ģ�ˡ������֪ͨ��
*/
    G("-g"),
    G_NONE("-g:none"),
    G_CUSTOM("-g:{lines,vars,source}"),
    XLINT("-Xlint"),
    XLINT_CUSTOM("-Xlint:{"
                 + "all,"
                 + "cast,deprecation,divzero,empty,unchecked,fallthrough,path,serial,finally,overrides,"
                 + "-cast,-deprecation,-divzero,-empty,-unchecked,-fallthrough,-path,-serial,-finally,-overrides,"
                 + "none}"),
    NOWARN("-nowarn"),
    VERBOSE("-verbose"),
    DEPRECATION("-deprecation"),
    CLASSPATH("-classpath"),
    CP("-cp"),
    SOURCEPATH("-sourcepath"),
    BOOTCLASSPATH("-bootclasspath"),
    XBOOTCLASSPATH_PREPEND("-Xbootclasspath/p:"),
    XBOOTCLASSPATH_APPEND("-Xbootclasspath/a:"),
    XBOOTCLASSPATH("-Xbootclasspath:"),
    EXTDIRS("-extdirs"),
    DJAVA_EXT_DIRS("-Djava.ext.dirs="),
    ENDORSEDDIRS("-endorseddirs"),
    DJAVA_ENDORSED_DIRS("-Djava.endorsed.dirs="),
    PROC_CUSTOM("-proc:{none,only}"),
    PROCESSOR("-processor"),
    PROCESSORPATH("-processorpath"),
    D("-d"),
    S("-s"),
    IMPLICIT("-implicit:{none,class}"),//1.7������׼ѡ�ָ���Ƿ�Ϊ��ʽ�����ļ��������ļ�
    ENCODING("-encoding"),
    SOURCE("-source"),
    TARGET("-target"),
    VERSION("-version"),
    FULLVERSION("-fullversion"),//����ѡ��(�ڲ�ʹ�ã�������ʾ)
    HELP("-help"),
    A("-A"),
    X("-X"),
    J("-J"),
    MOREINFO("-moreinfo"),//����ѡ��(�ڲ�ʹ�ã�������ʾ)
    WERROR("-Werror"),//����ѡ��(�ڲ�ʹ�ã�������ʾ)
    COMPLEXINFERENCE("-complexinference"),//����ѡ��(�ڲ�ʹ�ã�������ʾ)
    PROMPT("-prompt"),//����ѡ��(�ڲ�ʹ�ã�������ʾ)
    DOE("-doe"),//����ѡ��(�ڲ�ʹ�ã�������ʾ)
    PRINTSOURCE("-printsource"),//����ѡ��(�ڲ�ʹ�ã�������ʾ)
    WARNUNCHECKED("-warnunchecked"),//����ѡ��(�ڲ�ʹ�ã�������ʾ)
    XMAXERRS("-Xmaxerrs"),
    XMAXWARNS("-Xmaxwarns"),
    XSTDOUT("-Xstdout"),
    XPRINT("-Xprint"),
    XPRINTROUNDS("-XprintRounds"),
    XPRINTPROCESSORINFO("-XprintProcessorInfo"),
    XPREFER("-Xprefer:{source,newer}"),//1.7������չѡ�ָ����ȡ�ļ�����ͬʱ�ҵ���ʽ�������Դ�ļ������ļ�ʱ
    O("-O"),//����ѡ��(�ڲ�ʹ�ã�������ʾ)
    XJCOV("-Xjcov"),//����ѡ��(�ڲ�ʹ�ã�������ʾ)
    XD("-XD"),//����ѡ��(�ڲ�ʹ�ã�������ʾ)
    SOURCEFILE("sourcefile");//����ѡ��(�ڲ�ʹ�ã�������ʾ)

    public final String optionName;

    OptionName(String optionName) {
        this.optionName = optionName;
    }

    @Override
    public String toString() {
        return optionName;
    }

}
