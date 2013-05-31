package com.bt.pi.core.past;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import rice.p2p.past.PastContent;

import com.bt.pi.core.past.content.DhtContentHeader;
import com.bt.pi.core.past.content.KoalaContentBase;
import com.bt.pi.core.scope.NodeScope;

public class PiBackupInsertionRunner implements Runnable {
    private static final Log LOG = LogFactory.getLog(PiBackupInsertionRunner.class);
    private PastContent content;
    private PiBackupHelper backupHelper;

    public PiBackupInsertionRunner(PastContent pastContent, PiBackupHelper piBackupHelper) {
        content = pastContent;
        backupHelper = piBackupHelper;
    }

    @Override
    public void run() {
        KoalaContentBase backupContent = (KoalaContentBase) content;
        if (LOG.isDebugEnabled())
            LOG.debug("Running backup task for content: " + content.getId());
        if (backupContent.getContentHeaders().containsKey(DhtContentHeader.BACKUPS)) {
            try {
                int numBackups = Integer.parseInt(backupContent.getContentHeaders().get(DhtContentHeader.BACKUPS));
                if (numBackups > 0) {
                    NodeScope scope = NodeScope.valueOf(backupContent.getContentHeaders().get(DhtContentHeader.BACKUP_SCOPE));
                    backupHelper.backupContent(numBackups, scope, backupContent);
                }
            } catch (NumberFormatException nfe) {
                LOG.warn(String.format("Unable to parse the number of backups required for content: %s. ", backupContent), nfe);
            } catch (IllegalArgumentException iae) {
                LOG.warn(String.format("Unable to parse the node scope for content: %s. ", backupContent), iae);
            } catch (NullPointerException npe) {
                LOG.warn(String.format("Backup scope for content: %s. was null. ", backupContent), npe);
            }

        }
    }
}
