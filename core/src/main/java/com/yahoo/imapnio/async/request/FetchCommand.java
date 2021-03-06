package com.yahoo.imapnio.async.request;

import javax.annotation.Nonnull;

import com.yahoo.imapnio.async.data.MessageNumberSet;

/**
 * This class defines imap fetch command request from client.
 */
public class FetchCommand extends AbstractFetchCommand {

    /**
     * Initializes a @{code FetchCommand} with the @{code MessageNumberSet} array and fetch items.
     *
     * @param msgsets the set of message set
     * @param items the data items
     */
    public FetchCommand(@Nonnull final MessageNumberSet[] msgsets, @Nonnull final String items) {
        super(false, msgsets, items);
    }

    /**
     * Initializes a @{code FetchCommand} with the @{code MessageNumberSet} array and macro.
     *
     * @param msgsets the set of message set
     * @param macro the macro
     */
    public FetchCommand(@Nonnull final MessageNumberSet[] msgsets, @Nonnull final FetchMacro macro) {
        super(false, msgsets, macro);
    }

    @Override
    public ImapCommandType getCommandType() {
        return ImapCommandType.FETCH;
    }
}
