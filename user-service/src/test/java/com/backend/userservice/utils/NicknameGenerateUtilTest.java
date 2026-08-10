package com.backend.userservice.utils;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("NicknameGenerateUtil 테스트")
class NicknameGenerateUtilTest {

    private static final Pattern NICKNAME_PATTERN = Pattern.compile("^.+_.+_\\d{5}$");

    @Test
    void 형용사_명사_5자리숫자_형식으로_생성한다() {
        String nickname = NicknameGenerateUtil.generate();

        Assertions.assertThat(nickname).matches(NICKNAME_PATTERN);
    }

    @Test
    void 여러_번_호출해도_비어있지_않은_닉네임을_반환한다() {
        Set<String> nicknames = new HashSet<>();
        for (int i = 0; i < 20; i++) {
            String nickname = NicknameGenerateUtil.generate();
            Assertions.assertThat(nickname).isNotBlank();
            Assertions.assertThat(nickname).matches(NICKNAME_PATTERN);
            nicknames.add(nickname);
        }

        Assertions.assertThat(nicknames).isNotEmpty();
    }
}
