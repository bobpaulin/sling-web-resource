package org.apache.sling.webresource.util;

import java.util.Map;

import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.webresource.model.GlobalCompileOptions;

public class ScriptUtils {

	/**
	 * Transforms a java multi-line string into javascript multi-line string.
	 * This technique was found at {@link http
	 * ://stackoverflow.com/questions/805107/multiline-strings-in-javascript/}
	 * 
	 * @param data
	 *            a string containing new lines.
	 * @return a string which being evaluated on the client-side will be treated
	 *         as a correct multi-line string.
	 */
	public static String toJSMultiLineString(final String data) {
		final String[] lines = data.split("\n");
		final StringBuffer result = new StringBuffer("[");
		if (lines.length == 0) {
			result.append("\"\"");
		}
		for (int i = 0; i < lines.length; i++) {
			final String line = lines[i];
			result.append("\"");
			result.append(line.replace("\\", "\\\\").replace("\"", "\\\"")
					.replaceAll("\\r|\\n", ""));
			// this is used to force a single line to have at least one new line
			// (otherwise cssLint fails).
			if (lines.length == 1) {
				result.append("\\n");
			}
			result.append("\"");
			if (i < lines.length - 1) {
				result.append(",");
			}
		}
		result.append("].join(\"\\n\")");
		return result.toString();
	}

	/**
	 * 
	 * Change compile options into a string
	 * 
	 * @param compileOptions
	 * @return
	 */
	public static String generateCompileOptionsString(
			Map<String, Object> compileOptions) {
		JSONObject keysJson = new JSONObject(compileOptions);
		return keysJson.toString();
	}

	/**
	 * 
	 * Gets global compile options.
	 * 
	 * @param compileOptions
	 * @return
	 */
	public static GlobalCompileOptions getGlobalCompileOptions(
			Map<String, Object> compileOptions) {
		GlobalCompileOptions result = null;
		if (compileOptions != null) {
			result = (GlobalCompileOptions) compileOptions.get("global");
		}

		return result;
	}
}
