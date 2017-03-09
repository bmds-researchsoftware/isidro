package edu.dartmouth.geisel.isidro.checksum;

import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Patrick Eads
 */
public final class ExcelChecksum
{
    /**
     * Checksum algorithm.
     */
    private final static String SHA512 = "sha-512";
    /**
     * Regex pattern for special characters.
     */
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern
            .compile("[\r\n\",]+");

    /**
     * Private constructor of utilities class.
     */
    private ExcelChecksum()
    {
    }

    /**
     * creates checksum hash of the given file
     *
     * @param csvContents
     *            2D list of csv contents as strings.
     * @throws IOException,
     *             {@link NoSuchAlgorithmException}
     */
    public static String checksum(final List<List<String>> csvContents)
            throws IOException, NoSuchAlgorithmException
    {
        String contents = "";
        for (final List<String> row : csvContents)
        {
            final java.util.Iterator<String> colIterator = row.iterator();
            while (colIterator.hasNext())
            {
                String content = colIterator.next();
                final Matcher matcher = SPECIAL_CHAR_PATTERN.matcher(content);
                content = content.replaceAll("\"", "\"\"");
                contents += matcher.find() ? "\"" + content + "\"" : content;
                if (colIterator.hasNext())
                {
                    contents += ",";
                }
            }
            contents += "\n";
        }
        final MessageDigest md = MessageDigest.getInstance(SHA512);
        final byte[] checksum = md.digest(contents.getBytes());
        final String result = String.format("%02x",
                new BigInteger(1, checksum));

        return result;
    }

}
