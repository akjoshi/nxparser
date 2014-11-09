package org.semanticweb.yars.nx.util;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Some utility methods that used to be spread over a couple of other classes.
 * 
 * @author Tobias Kaefer
 * @author others that wrote the methods in the first place
 * @author Leonard Lausen
 */
public class NxUtil {
	private static final Pattern IRIPATTERN = Pattern
			.compile("^([^:/?#]+)://([^/?#]*)?([^?#]*)(?:\\?([^#]*))?(?:#(.*))?");
	private static final Pattern PERCENTPATTERN = Pattern
			.compile("%[\\dA-Fa-f]{2}");

	private NxUtil() {

	}

/**
	 * Escape IRI according to RDF 1.1 N-Triples W3C Recommendation 25 February 2014
	 * 
	 * IRIREF	::= 	'<' ([^#x00-#x20<>"{}|^`\] | UCHAR)* '>'
	 * 
	 * @param iri IRI to escape
	 * @return escaped IRI
	 * @see <a href="http://www.w3.org/TR/2014/REC-n-triples-20140225/">The N-Triples spec</a>
	 */
	public static String escapeIRI(String iri) {
		StringBuffer result = new StringBuffer();

		final int length = iri.length();
		for (int offset = 0; offset < length;) {
			final int codepoint = iri.codePointAt(offset);

			if (!Character.isSupplementaryCodePoint(codepoint)) {
				char c = (char) codepoint;

				switch (c) {
				case '\\':
					result.append("\\u005C");
					break;
				case '`':
					result.append("\\u0060");
					break;
				case '^':
					result.append("\\u005E");
					break;
				case '|':
					result.append("\\u007C");
					break;
				case '{':
					result.append("\\u007B");
					break;
				case '}':
					result.append("\\u007D");
					break;
				case '"':
					result.append("\\u0022");
					break;
				case '<':
					result.append("\\u003C");
					break;
				case '>':
					result.append("\\u003E");
					break;
				default:
					if (c <= 32) {

						result.append(String.format("\\u%04X", (int) c));
					} else {
						result.append(c);
					}
				}
			} else {
				result.appendCodePoint(codepoint);
			}

			offset += Character.charCount(codepoint);
		}

		return result.toString();
	}

/**
	 * Unescape IRI according to RDF 1.1 N-Triples W3C Recommendation 25 February 2014
	 * 
	 * IRIREF	::= 	'<' ([^#x00-#x20<>"{}|^`\] | UCHAR)* '>'
	 * 
	 * @param str IRI to escape
	 * @return unescaped IRI
	 */
	public static String unescapeIRI(String str) {
		int sz = str.length();

		StringBuffer buffer = new StringBuffer(sz);
		StringBuffer unicode = new StringBuffer(6);

		boolean hadSlash = false;
		boolean inUnicode = false;
		boolean inSpecialUnicode = false;

		for (int i = 0; i < sz; i++) {
			char ch = str.charAt(i);
			if (inUnicode) {
				// if in unicode, then we're reading unicode
				// values in somehow

				if (unicode.length() < 4) {
					unicode.append(ch);

					if (unicode.length() == 4) {
						// unicode now contains the four hex digits
						// which represents our unicode chacater
						try {
							int value = Integer
									.parseInt(unicode.toString(), 16);
							buffer.append((char) value);
							unicode = new StringBuffer(4);
							inUnicode = false;
							inSpecialUnicode = false;
							hadSlash = false;
						} catch (NumberFormatException nfe) {
							buffer.append(unicode.toString());
							continue;
						}
						continue;
					}
					continue;
				}

			}

			if (inSpecialUnicode) {
				// if in unicode, then we're reading unicode
				// values in somehow

				if (unicode.length() < 8) {
					unicode.append(ch);

					if (unicode.length() == 8) {
						// unicode now contains the six hex digits
						// which represents our code point
						try {
							buffer.appendCodePoint(Integer.parseInt(
									unicode.toString(), 16));

							unicode = new StringBuffer(8);
							inUnicode = false;
							inSpecialUnicode = false;
							hadSlash = false;
						} catch (NumberFormatException nfe) {
							buffer.append(unicode.toString());
							continue;
						}
						continue;
					}
					continue;
				}

			}

			if (hadSlash) {
				// handle an escaped value
				hadSlash = false;
				switch (ch) {
				case 'u': {
					// uh-oh, we're in unicode country....
					inUnicode = true;
					break;
				}
				case 'U': {
					// even more uh-oh, we're in special unicode land...
					inSpecialUnicode = true;
					break;
				}
				default:
					buffer.append('\\' + ch);
					break;
				}
				continue;
			}

			else if (ch == '\\') {
				hadSlash = true;
				continue;
			}

			buffer.append(ch);
		}

		if (hadSlash) {
			// then we're in the weird case of a \ at the end of the
			// string, let's output it anyway.
			buffer.append('\\');
		}
		return buffer.toString();
	}

	public static String escapeLiteral(String lit) {
		StringBuffer result = new StringBuffer();

		final int length = lit.length();
		for (int offset = 0; offset < length;) {
			final int codepoint = lit.codePointAt(offset);

			if (!Character.isSupplementaryCodePoint(codepoint)) {
				char c = (char) codepoint;

				switch (c) {
				case '\\':
					result.append("\\\\");
					break;
				case '"':
					result.append("\\\"");
					break;
				case 10:
					result.append("\\n");
					break;
				case 13:
					result.append("\\r");
					break;
				default:
					result.append(c);
				}
			} else {
				result.appendCodePoint(codepoint);
			}

			offset += Character.charCount(codepoint);
		}

		return result.toString();
	}

	/**
	 * Unescape special characters in literal.
	 * 
	 * @param str
	 *            The string to escape
	 */
	public static String unescapeLiteral(String str) {
		int sz = str.length();

		StringBuffer buffer = new StringBuffer(sz);
		StringBuffer unicode = new StringBuffer(6);

		boolean hadSlash = false;
		boolean inUnicode = false;
		boolean inSpecialUnicode = false;

		for (int i = 0; i < sz; i++) {
			char ch = str.charAt(i);
			if (inUnicode) {
				// if in unicode, then we're reading unicode
				// values in somehow

				if (unicode.length() < 4) {
					unicode.append(ch);

					if (unicode.length() == 4) {
						// unicode now contains the four hex digits
						// which represents our unicode chacater
						try {
							int value = Integer
									.parseInt(unicode.toString(), 16);
							buffer.append((char) value);
							unicode = new StringBuffer(4);
							inUnicode = false;
							inSpecialUnicode = false;
							hadSlash = false;
						} catch (NumberFormatException nfe) {
							buffer.append(unicode.toString());
							continue;
						}
						continue;
					}
					continue;
				}

			}

			if (inSpecialUnicode) {
				// if in unicode, then we're reading unicode
				// values in somehow

				if (unicode.length() < 8) {
					unicode.append(ch);

					if (unicode.length() == 8) {
						// unicode now contains the six hex digits
						// which represents our code point
						try {
							buffer.appendCodePoint(Integer.parseInt(
									unicode.toString(), 16));

							unicode = new StringBuffer(8);
							inUnicode = false;
							inSpecialUnicode = false;
							hadSlash = false;
						} catch (NumberFormatException nfe) {
							buffer.append(unicode.toString());
							continue;
						}
						continue;
					}
					continue;
				}

			}

			if (hadSlash) {
				// handle an escaped value
				hadSlash = false;
				switch (ch) {
				case '\\':
					buffer.append('\\');
					break;
				case '\'':
					buffer.append('\'');
					break;
				case '\"':
					buffer.append('"');
					break;
				case 'r':
					buffer.append('\r');
					break;
				case 'f':
					buffer.append('\f');
					break;
				case 't':
					buffer.append('\t');
					break;
				case 'n':
					buffer.append('\n');
					break;
				case 'b':
					buffer.append('\b');
					break;
				case 'u': {
					// uh-oh, we're in unicode country....
					inUnicode = true;
					break;

				}
				case 'U': {
					// even more uh-oh, we're in special unicode land...
					inSpecialUnicode = true;
					break;
				}

				default:
					buffer.append('\\' + ch);
					break;
				}
				continue;
			}

			else if (ch == '\\') {
				hadSlash = true;
				continue;
			}

			buffer.append(ch);
		}

		if (hadSlash) {
			// then we're in the weird case of a \ at the end of the
			// string, let's output it anyway.
			buffer.append('\\');
		}
		return buffer.toString();
	}

	/**
	 * Escapes strings to unicode. Note that this method does not all the work
	 * required by the spec for processing URIs. {@link URI#toASCIIString()}
	 * could be your friend here.
	 * 
	 * @see <a
	 *      href="http://www.w3.org/TR/2004/REC-rdf-testcases-20040210/#ntrip_strings">What
	 *      the N-Triples 1.0 spec says about the encoding of strings</a>
	 * @see <a
	 *      href="http://www.w3.org/TR/2004/REC-rdf-testcases-20040210/#sec-uri-encoding">What
	 *      the N-Triples 1.0 spec says on the encoding of URIs</a>
	 * @deprecated This method escapes for N-Triples 1.0,
	 *             {@link #escapeLiteral(String)} and {@link #escapeIRI(String)}
	 *             are the new methods supporting N-Triples 1.1
	 */
	@Deprecated
	public static String escapeForNTriples1(String lit) {
		StringBuffer result = new StringBuffer();

		for (int i = 0; i < lit.length(); i++) {

			int cp = lit.codePointAt(i);
			char c;

			if (!Character.isSupplementaryCodePoint(cp)) {
				c = (char) (cp);
				switch (c) {
				case '\\':
					result.append("\\\\");
					break;
				case '"':
					result.append("\\\"");
					break;
				case '\n':
					result.append("\\n");
					break;
				case '\r':
					result.append("\\r");
					break;
				case '\t':
					result.append("\\t");
					break;
				default:
					if (c >= 0x0 && c <= 0x8 || c == 0xB || c == 0xC
							|| c >= 0xE && c <= 0x1F || c >= 0x7F
							&& c <= 0xFFFF) {
						result.append("\\u");
						result.append(toHexString(c, 4));
					} else {
						result.append(c);
					}
				}
			} else {
				result.append("\\U");
				result.append(toHexString(cp, 8));
				++i;
			}

		}

		return result.toString();
	}

	/**
	 * Escapes strings for markup.
	 * 
	 */
	public static String escapeForMarkup(String lit) {
		String unescaped = unescapeLiteral(lit);

		StringBuffer result = new StringBuffer();

		for (int i = 0; i < unescaped.length(); i++) {
			int cp = unescaped.codePointAt(i);
			char c;

			if (!Character.isSupplementaryCodePoint(cp)) {
				c = (char) (cp);

				switch (c) {
				case '&':
					result.append("&amp;");
					break;
				case '<':
					result.append("&lt;");
					break;
				case '>':
					result.append("&gt;");
					break;
				case '\"':
					result.append("&quot;");
					break;
				case '\'':
					result.append("&#039;");
					break;
				case '\\':
					result.append("&#092;");
					break;
				default:
					if (c >= 0x0 && c <= 0x8 || c == 0xB || c == 0xC
							|| c >= 0xE && c <= 0x1F || c >= 0x7F
							&& c <= 0xFFFF) {
						result.append("&#x");
						result.append(toHexString(c, 4));
						result.append(";");
					} else {
						result.append(c);
					}
				}
			} else {
				result.append("&#x");
				result.append(toHexString(cp, 8));
				result.append(";");
				++i;
			}
		}
		return result.toString();
	}

	/**
	 * Converts a decimal value to a hexadecimal string represention of the
	 * specified length. For unicode escaping.
	 * 
	 * @param decimal
	 *            A decimal value.
	 * @param stringLength
	 *            The length of the resulting string.
	 **/
	private static String toHexString(int decimal, int stringLength) {
		return String.format("%0" + stringLength + "X", decimal);
	}

	/**
	 * Unescape special characters in N-Triples 1.0 Literals or Resources.
	 * 
	 * @param str
	 *            The string to escape
	 * @deprecated This method unescapes for N-Triples 1.0, stuff that has been
	 *             escaped using {@link #escapeForNTriples1(String)}.
	 *             {@link #unescapeLiteral(String)} and
	 *             {@link #unescapeIRI(String)} are the new methods supporting
	 *             N-Triples 1.1
	 * 
	 */
	public static String unescapeForNTriples1(String str) {
		return unescapeForNTriples1(str, false);
	}

	/**
	 * Unescape special characters in N-Triples 1.0 Literals or Resources.
	 * 
	 * @param str
	 *            The string to escape
	 * @param clean
	 *            If true, cleans up excess slashes
	 * @deprecated This method unescapes for N-Triples 1.0, stuff that has been
	 *             escaped using {@link #escapeForNTriples1(String)}.
	 *             {@link #unescapeLiteral(String)} and
	 *             {@link #unescapeIRI(String)} are the new methods supporting
	 *             N-Triples 1.1
	 */
	public static String unescapeForNTriples1(String str, boolean clean) {
		if (clean)
			str = cleanSlashes(str);
		int sz = str.length();

		StringBuffer buffer = new StringBuffer(sz);
		StringBuffer unicode = new StringBuffer(6);

		boolean hadSlash = false;
		boolean inUnicode = false;
		boolean inSpecialUnicode = false;

		for (int i = 0; i < sz; i++) {
			char ch = str.charAt(i);
			if (inUnicode) {
				// if in unicode, then we're reading unicode
				// values in somehow

				if (unicode.length() < 4) {
					unicode.append(ch);

					if (unicode.length() == 4) {
						// unicode now contains the four hex digits
						// which represents our unicode chacater
						try {
							int value = Integer
									.parseInt(unicode.toString(), 16);
							buffer.append((char) value);
							unicode = new StringBuffer(4);
							inUnicode = false;
							inSpecialUnicode = false;
							hadSlash = false;
						} catch (NumberFormatException nfe) {
							buffer.append(unicode.toString());
							continue;
						}
						continue;
					}
					continue;
				}

			}

			if (inSpecialUnicode) {
				// if in unicode, then we're reading unicode
				// values in somehow

				if (unicode.length() < 8) {
					unicode.append(ch);

					if (unicode.length() == 8) {
						// unicode now contains the six hex digits
						// which represents our code point
						try {
							buffer.appendCodePoint(Integer.parseInt(
									unicode.toString(), 16));

							unicode = new StringBuffer(8);
							inUnicode = false;
							inSpecialUnicode = false;
							hadSlash = false;
						} catch (NumberFormatException nfe) {
							buffer.append(unicode.toString());
							continue;
						}
						continue;
					}
					continue;
				}

			}

			if (hadSlash) {
				// handle an escaped value
				hadSlash = false;
				switch (ch) {
				case '\\':
					buffer.append('\\');
					break;
				case '\'':
					buffer.append('\'');
					break;
				case '\"':
					buffer.append('"');
					break;
				case 'r':
					buffer.append('\r');
					break;
				case 'f':
					buffer.append('\f');
					break;
				case 't':
					buffer.append('\t');
					break;
				case 'n':
					buffer.append('\n');
					break;
				case 'b':
					buffer.append('\b');
					break;
				case 'u': {
					// uh-oh, we're in unicode country....
					inUnicode = true;
					break;

				}
				case 'U': {
					// even more uh-oh, we're in special unicode land...
					inSpecialUnicode = true;
					break;
				}

				default:
					buffer.append(ch);
					break;
				}
				continue;
			}

			else if (ch == '\\') {
				hadSlash = true;
				continue;
			}

			buffer.append(ch);
		}

		if (hadSlash) {
			// then we're in the weird case of a \ at the end of the
			// string, let's output it anyway.
			buffer.append('\\');
		}
		return buffer.toString();
	}

	/**
	 * Remove multiples of \\ for cleaning data escaped multiple times.
	 */
	private static String cleanSlashes(String str) {
		while (str.indexOf("\\\\") != -1)
			str = str.replaceAll("\\\\\\\\", "\\\\");
		return str;
	}

	/**
	 * Normalize IRI using techniques from RFC3987 5.1
	 * 
	 * @param iri
	 * @return normalized form of iri
	 */
	public static String normalize(String iri) {
		if (!Normalizer.isNormalized(iri, Normalizer.Form.NFC)) {
			iri = Normalizer.normalize(iri, Normalizer.Form.NFC);
		}

		String[] iria = splitIRI(iri);
		StringBuilder b = new StringBuilder();
		b.append(iria[0].toLowerCase());
		b.append("://");
		b.append(unescapePercentEncoding(iria[1], false).toLowerCase());
		b.append(unescapePercentEncoding(iria[2], false));
		if (iria[3] != "")
			b.append("?");
		b.append(unescapePercentEncoding(iria[3], true));
		if (iria[4] != "")
			b.append("#");
		b.append(unescapePercentEncoding(iria[4], false));
		return b.toString();
	}

	/**
	 * @param iri
	 * @return array of scheme, authority, path, query, fragment of iri
	 */
	public static String[] splitIRI(String iri) {
		Matcher irim = IRIPATTERN.matcher(iri);
		String[] a = new String[5];
		if (irim.find()) {
			for (int i = 1; i <= 5; i++) {
				String s = irim.group(i);
				if (s == null)
					a[i - 1] = "";
				else
					a[i - 1] = s;
			}
			return a;
		} else
			return null;

	}

	/**
	 * @param iri
	 * @return authority part of iri
	 */
	public static String getAuthority(String iri) {
		Matcher irim = IRIPATTERN.matcher(iri);
		if (irim.find()) {
			return irim.group(2);
		} else
			return null;
	}

	/**
	 * @param iri
	 * @return scheme part of iri
	 */
	public static String getScheme(String iri) {
		Matcher irim = IRIPATTERN.matcher(iri);
		if (irim.find()) {
			return irim.group(1);
		} else
			return null;
	}

	/**
	 * @param iri
	 * @return query part of iri
	 */
	public static String getQuery(String iri) {
		Matcher irim = IRIPATTERN.matcher(iri);
		if (irim.find()) {
			return irim.group(4);
		} else
			return null;
	}

	/**
	 * @param iri
	 * @return fragment part of iri
	 */
	public static String getFragment(String iri) {
		Matcher irim = IRIPATTERN.matcher(iri);
		if (irim.find()) {
			return irim.group(5);
		} else
			return null;
	}

	/**
	 * @param iri
	 * @return path part of iri
	 */
	public static String getPath(String iri) {
		Matcher irim = IRIPATTERN.matcher(iri);
		if (irim.find()) {
			return irim.group(3);
		} else
			return null;
	}

	/**
	 * Unescape percent encoding that is allowed in IRI. This method assumes,
	 * that the Percent-Encoded characters are UTF8 characters.
	 * 
	 * @param str
	 *            The string to unescape
	 * @param privateUCS
	 *            Unescape private UCS characters as well. They are only allowed
	 *            in the query part of an IRI, so be careful to only unescape
	 *            them there.
	 */
	public static String unescapePercentEncoding(String str, Boolean privateUCS) {
		StringBuilder result = new StringBuilder(str.length()); // Here will be
																// the decoded
																// string
		StringBuilder orig = new StringBuilder(); // Saves the original
													// percent-encoded string
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		String enc;
		int value;
		for (int i = 0; i < str.length();) {
			char c = str.charAt(i);

			// Percent-Encoded stuff ahead
			if (c == '%' && i + 2 < str.length()) {
				// Get and save the percent-encoded chars
				enc = str.substring(i + 1, i + 3);
				orig.append('%');
				orig.append(enc);

				// Decode and check if this is valid UTF8
				value = Integer.parseInt(enc, 16);
				if ((value & 0x80) == 0) { // 1-Byte UTF8
					bytes.write((byte) value);
				} else if ((value & 0x20) == 0 && i + 5 < str.length()
						&& str.charAt(i + 3) == '%') { // 2-Byte UTF8
					bytes.write((byte) value);
					i += 3;
					enc = str.substring(i + 1, i + 3);
					orig.append('%');
					orig.append(enc);
					bytes.write(Integer.parseInt(enc, 16));
				} else if ((value & 0x10) == 0 && i + 8 < str.length()
						&& str.charAt(i + 3) == '%' && str.charAt(i + 6) == '%') { // 3-Byte
																					// UTF8
					bytes.write((byte) value);
					for (int j = 0; j < 2; j++) {
						i += 3;
						enc = str.substring(i + 1, i + 3);
						orig.append('%');
						orig.append(enc);
						bytes.write(Integer.parseInt(enc, 16));
					}
				} else if ((value & 0x8) == 0 && i + 11 < str.length()
						&& str.charAt(i + 3) == '%' && str.charAt(i + 6) == '%'
						&& str.charAt(i + 9) == '%') { // 4-Byte UTF8
					bytes.write((byte) value);
					for (int j = 0; j < 3; j++) {
						i += 3;
						enc = str.substring(i + 1, i + 3);
						orig.append('%');
						orig.append(enc);
						bytes.write(Integer.parseInt(enc, 16));
					}
				} else { // No UTF8
					result.append(orig.toString());
					orig.setLength(0);
					orig.trimToSize();
					i += 3;
					continue;
				}

				// Parse the UTF8 to a Java String
				String s = new String(bytes.toByteArray(),
						StandardCharsets.UTF_8);
				assert s.codePointCount(0, s.length()) == 1; // there should
																// only be one
																// character
				ByteBuffer b;
				try {
					// Convert to UTF32BE, which is "identical" to Unicode Code
					// Points
					b = ByteBuffer.wrap(s.getBytes("UTF-32BE"));
					value = b.getInt();

					// Check whether character is allowed in IRI (RFC 3987 §2.2)
					if ((value >= 0x41 && value <= 0x5A)
							|| // Small alpha
							(value >= 0x61 && value <= 0x7A)
							|| // Big alpha
							(value >= 0x30 && value <= 0x39)
							|| // Digit
							(value == '-' || value == '.' || value == '_' || value == '~')
							|| (value >= 0xA0 && value <= 0xD7FF)
							|| // RFC 3987 ucschar
							(value >= 0xF900 && value <= 0xFDCF)
							|| (value >= 0xFDF0 && value <= 0xFEFF)
							|| (value >= 0x10000 && value <= 0x1FFFD)
							|| (value >= 0x20000 && value <= 0x2FFFD)
							|| (value >= 0x30000 && value <= 0x3FFFD)
							|| (value >= 0x40000 && value <= 0x4FFFD)
							|| (value >= 0x50000 && value <= 0x5FFFD)
							|| (value >= 0x60000 && value <= 0x6FFFD)
							|| (value >= 0x70000 && value <= 0x7FFFD)
							|| (value >= 0x80000 && value <= 0x8FFFD)
							|| (value >= 0x90000 && value <= 0x9FFFD)
							|| (value >= 0xA0000 && value <= 0xAFFFD)
							|| (value >= 0xB0000 && value <= 0xBFFFD)
							|| (value >= 0xC0000 && value <= 0xCFFFD)
							|| (value >= 0xD0000 && value <= 0xDFFFD)
							|| (value >= 0xE1000 && value <= 0xEFFFD)) {
						result.append(Character.toChars(value));
					} // If enabled, check if character is in private UCS range
					else if (privateUCS) {
						if ((value >= 0xE000 && value <= 0xF8FF)
								|| (value >= 0xF0000 && value <= 0xFFFFD)
								|| (value >= 0x100000 && value <= 0x10FFFD)) {
							result.append(Character.toChars(value));
						} else {
							result.append(orig.toString().toUpperCase());
						}
					} else {
						result.append(orig.toString().toUpperCase());
					}
				} catch (NumberFormatException e) {
					// It's not possible to decode, so leave the original
					// percent-encoding
					result.append(orig.toString().toUpperCase());

				} catch (UnsupportedEncodingException e) {
					// It's not possible to decode, so leave the original
					// percent-encoding
					result.append(orig.toString().toUpperCase());
				}

				orig.setLength(0);
				orig.trimToSize();
				bytes.reset();
				i += 3;
			} else {
				result.append(c);
				i++;
			}
		}

		return result.toString();
	}

	/**
	 * Case Normalize percent encoding.
	 * 
	 * @param str
	 *            The string to normalize
	 */
	public static String caseNormalizePercentEncoding(String str) {
		StringBuffer buffer = new StringBuffer();
		Matcher m = PERCENTPATTERN.matcher(str);
		int last = 0;
		String enc;
		while (m.find()) {
			buffer.append(str.substring(last, m.start()));
			enc = m.group();
			last = m.end();
			buffer.append(enc.toUpperCase());
		}
		buffer.append(str.substring(last));
		return buffer.toString();
	}
}
