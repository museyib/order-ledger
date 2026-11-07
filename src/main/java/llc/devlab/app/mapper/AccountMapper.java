package llc.devlab.app.mapper;

import llc.devlab.app.model.dto.AccountSummaryDto;
import llc.devlab.app.model.entity.Account;
import org.mapstruct.Mapper;

@Mapper
public interface AccountMapper {

    AccountSummaryDto toSummary(Account account);
}
