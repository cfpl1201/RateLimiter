package com.dbq.filter;

import java.util.regex.Pattern;

/**
 * xss过滤规则
 *
 * @author dbq
 * @date 2020/7/13 17:02
 */
public class XssCleanRuleUtils {
	private final static Pattern[] scriptPatterns = {
			Pattern.compile("<script>(.*?)</script>", Pattern.CASE_INSENSITIVE),
			Pattern.compile("src[\r\n]*=[\r\n]*\\\'(.*?)\\\'", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
			Pattern.compile("</script>", Pattern.CASE_INSENSITIVE),
			Pattern.compile("<script(.*?)>", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
			Pattern.compile("eval\\((.*?)\\)", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
			Pattern.compile("expression\\((.*?)\\)", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
			Pattern.compile("javascript:", Pattern.CASE_INSENSITIVE),
			Pattern.compile("vbscript:", Pattern.CASE_INSENSITIVE),
			Pattern.compile("onload(.*?)=", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL)
	};
	public static String xssClean(String value) {
		if (value != null) {
			value = value.replaceAll("\0|\n|\r", "");
			for (Pattern pattern : scriptPatterns) {
				value = pattern.matcher(value).replaceAll("");
			}
			value = value.replaceAll("<", "&lt;").replaceAll(">", "&gt;");
		}
		return value;

	}
}
