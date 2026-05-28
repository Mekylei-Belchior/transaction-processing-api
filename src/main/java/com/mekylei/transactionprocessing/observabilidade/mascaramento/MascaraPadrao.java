package com.mekylei.transactionprocessing.observabilidade.mascaramento;

import java.util.regex.Pattern;

public record MascaraPadrao(Pattern pattern, String substituto) {
}