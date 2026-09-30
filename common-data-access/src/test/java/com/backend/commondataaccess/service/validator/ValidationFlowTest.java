package com.backend.commondataaccess.service.validator;

import com.backend.commondataaccess.exception.BadRequestException;
import java.util.ArrayList;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ValidationFlow 테스트")
class ValidationFlowTest {

    @Test
    void end를_호출하면_등록한_순서대로_검증을_실행한다() {
        List<String> calls = new ArrayList<>();

        ValidationFlow.start("target")
                      .next(target -> {
                          calls.add("first:" + target);
                          return target;
                      })
                      .next(target -> {
                          calls.add("second:" + target);
                          return target;
                      })
                      .end();

        Assertions.assertThat(calls).containsExactly("first:target", "second:target");
    }

    @Test
    void end를_호출하기_전에는_검증을_실행하지_않는다() {
        List<String> calls = new ArrayList<>();

        ValidationFlow.start("target")
                      .next(target -> {
                          calls.add(target);
                          return target;
                      });

        Assertions.assertThat(calls).isEmpty();
    }

    @Test
    void 중간_검증이_실패하면_이후_검증은_실행하지_않는다() {
        List<String> calls = new ArrayList<>();

        ValidationFlow<String> flow = ValidationFlow.start("target")
                                                    .next(target -> {
                                                        throw new BadRequestException("invalid");
                                                    })
                                                    .next(target -> {
                                                        calls.add(target);
                                                        return target;
                                                    });

        Assertions.assertThatThrownBy(flow::end)
                  .isInstanceOf(BadRequestException.class)
                  .hasMessage("invalid");
        Assertions.assertThat(calls).isEmpty();
    }
}
