package com.bgsistemas.api.twofactor.dtos;

import java.util.List;

// BGSI-276: the 10 recovery codes, shown only once
public record RecoveryCodesDTO(List<String> recoveryCodes) {
}
