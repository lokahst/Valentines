package lokahst.valentines.data;

import java.util.UUID;

public record MarriageProposal(UUID proposer, UUID target, long proposalDate) {

    public boolean isExpired(long timeoutMs) {
        return System.currentTimeMillis() - proposalDate > timeoutMs;
    }
}