package edu.dartmouth.geisel.isidro.read;

public final class DataIntegrityMismatchException extends Exception
{
    /**
     * UID for Serializable.
     */
    private static final long serialVersionUID = 7078245627397282279L;

    /**
     * Exception to be thrown if there is a failure in checking data integrity.
     * 
     * @param message
     *            String message for data integrity failure.
     */
    public DataIntegrityMismatchException(final String message)
    {
        super(message);
    }

}
