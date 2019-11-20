package com.yahoo.imapnio.async.request;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.annotation.Nonnull;

import org.apache.commons.codec.binary.Base64;

import com.sun.mail.imap.protocol.IMAPResponse;
import com.yahoo.imapnio.async.data.Capability;
import com.yahoo.imapnio.async.exception.ImapAsyncClientException;
import com.yahoo.imapnio.async.exception.ImapAsyncClientException.FailureType;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * This class defines imap authenticate xoauth2 command request from client.
 */
public class AuthOauthBearerCommand extends ImapRequestAdapter {

    /** Byte array for CR and LF, keeping the array local so it cannot be modified by others. */
    private static final byte[] CRLF_B = { '\r', '\n' };

    /** Command operator. */
    private static final String AUTH_OAUTHBEARER = "AUTHENTICATE OAUTHBEARER";

    /** Byte array for AUTH OAUTHBEARER. */
    private static final byte[] AUTH_OAUTHBEARER_B = AUTH_OAUTHBEARER.getBytes(StandardCharsets.US_ASCII);

    /** AUTH_OAUTHBEARER length. */
    private static final int AUTH_OAUTHBEARER_LEN = AUTH_OAUTHBEARER.length();

    /** Literal for logging data. */
    private static final String LOG_PREFIX = "AUTHENTICATE OAUTHBEARER FOR USER:";

    /** Literal for user=. */
    private static final String N_A = "n,a=";

    /** Literal for auth==Bearer. */
    private static final String AUTH_BEARER = "auth=Bearer ";

    /** Extra length for port and a bunch of SOH. */
    private static final int EXTRA_LEN = 50;

    /** Comma literal. */
    private static final char COMMA = ',';

    /** Email Id. */
    private String emailId;

    /** Host name. */
    private String hostname;

    /** Port. */
    private int port;

    /** User token. */
    private String token;

    /** flag whether server allows one liner (Refer to RFC4959) instead of server challenge. */
    private boolean isSaslIREnabled;

    /**
     * Initializes an authenticate xoauth2 command.
     *
     * @param emailId the user name
     * @param hostname the host name
     * @param port the port
     * @param token xoauth2 token
     * @param capa the capability obtained from server
     */
    public AuthOauthBearerCommand(@Nonnull final String emailId, @Nonnull final String hostname, final int port, @Nonnull final String token,
            @Nonnull final Capability capa) {
        this.emailId = emailId;
        this.hostname = hostname;
        this.port = port;
        this.token = token;
        this.isSaslIREnabled = capa.hasCapability(ImapClientConstants.SASL_IR);
    }

    @Override
    public void cleanup() {
        this.emailId = null;
        this.hostname = null;
        this.token = null;
    }

    /**
     * Builds the IR, aka client Initial Response (RFC4959). In this command, it is Oauthbearer token format and encoded as base64.
     *
     * @return an encoded base64 Oauthbearer format
     */
    private String buildClientResponse() {
        // String format: n,a=user@example.com,^Ahost=server.example.com^Aport=993^Aauth=Bearer <oauthtoken>^A^A
        final int len = N_A.length() + emailId.length() + hostname.length() + token.length() + EXTRA_LEN;
        final StringBuilder sbOauth2 = new StringBuilder(len).append(N_A).append(emailId).append(COMMA).append(ImapClientConstants.SOH);
        sbOauth2.append("host=").append(hostname).append(ImapClientConstants.SOH).append("port=").append(port).append(ImapClientConstants.SOH);
        sbOauth2.append(AUTH_BEARER).append(token).append(ImapClientConstants.SOH).append(ImapClientConstants.SOH);
        return Base64.encodeBase64String(sbOauth2.toString().getBytes(StandardCharsets.US_ASCII));
    }

    @Override
    public ByteBuf getCommandLineBytes() {
        if (isSaslIREnabled) { // server allows client response in one line
            final String clientResp = buildClientResponse();
            final ByteBuf sb = Unpooled.buffer(clientResp.length() + ImapClientConstants.PAD_LEN);
            sb.writeBytes(AUTH_OAUTHBEARER_B);
            sb.writeByte(ImapClientConstants.SPACE);
            sb.writeBytes(clientResp.getBytes(StandardCharsets.US_ASCII));
            sb.writeBytes(CRLF_B);
            return sb;
        }
        final int len = AUTH_OAUTHBEARER_LEN + ImapClientConstants.CRLFLEN;
        final ByteBuf buf = Unpooled.buffer(len);
        buf.writeBytes(AUTH_OAUTHBEARER_B);
        buf.writeBytes(CRLF_B);
        return buf;
    }

    @Override
    public boolean isCommandLineDataSensitive() {
        return true;
    }

    @Override
    public String getDebugData() {
        return new StringBuilder(LOG_PREFIX).append(emailId).toString();
    }

    @Override
    public ConcurrentLinkedQueue<IMAPResponse> getStreamingResponsesQueue() {
        return null;
    }

    @Override
    public ByteBuf getNextCommandLineAfterContinuation(final IMAPResponse serverResponse) throws ImapAsyncClientException {
        if (isSaslIREnabled) { // should not reach here, since if SASL-IR enabled, server should not ask for next line
            throw new ImapAsyncClientException(FailureType.OPERATION_NOT_SUPPORTED_FOR_COMMAND);
        }
        final String clientResp = buildClientResponse();
        final ByteBuf buf = Unpooled.buffer(clientResp.length() + ImapClientConstants.CRLFLEN);
        buf.writeBytes(clientResp.getBytes(StandardCharsets.US_ASCII));
        buf.writeBytes(CRLF_B);
        return buf;
    }

    @Override
    public ByteBuf getTerminateCommandLine() throws ImapAsyncClientException {
        throw new ImapAsyncClientException(FailureType.OPERATION_NOT_SUPPORTED_FOR_COMMAND);
    }

    @Override
    public ImapCommandType getCommandType() {
        return ImapCommandType.AUTHENTICATE;
    }
}