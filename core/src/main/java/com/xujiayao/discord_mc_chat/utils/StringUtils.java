package com.xujiayao.discord_mc_chat.utils;

/**
 * String utility class.
 *
 * @author Xujiayao
 */
public class StringUtils {

	/**
	 * Escape special characters in strings.
	 *
	 * @param s String to escape
	 * @return Escaped string
	 */
	public static String escape(String s) {
		return s.replace("\t", "\\t")
				.replace("\b", "\\b")
				.replace("\n", "\\n")
				.replace("\r", "\\r")
				.replace("\f", "\\f");
	}

	/**
	 * Formats a string with placeholders.
	 * <p>
	 * Supports both sequential "{}" and indexed "{0}", "{1}" placeholders.
	 * Note: Do not mix both styles in the same string.
	 *
	 * @param str  String with placeholders
	 * @param args Arguments to replace the placeholders
	 * @return String with placeholders replaced
	 */
	public static String format(String str, Object... args) {
		if (str == null || args == null || args.length == 0) {
			return str;
		}

		// Check if the string uses indexed placeholders (e.g., "{0}", "{1}")
		// We assume that if it contains "{0}", it's using indexed mode.
		if (str.contains("{0}")) {
			for (int i = 0; i < args.length; i++) {
				String target = "{" + i + "}";
				String replacement = args[i] == null ? "null" : args[i].toString();
				str = str.replace(target, replacement);
			}
			return str;
		}

		// Otherwise, use sequential "{}" replacement
		StringBuilder sb = new StringBuilder(str.length());
		int searchStart = 0;
		int argIndex = 0;

		while (argIndex < args.length) {
			int placeholderIndex = str.indexOf("{}", searchStart);
			if (placeholderIndex == -1) {
				break;
			}

			sb.append(str, searchStart, placeholderIndex);
			sb.append(args[argIndex] == null ? "null" : args[argIndex].toString());

			searchStart = placeholderIndex + 2;
			argIndex++;
		}

		sb.append(str.substring(searchStart));
		return sb.toString();
	}
}
