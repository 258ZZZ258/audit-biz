package com.orientsec.idap.core.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.orientsec.idap.common.model.Result;
import com.orientsec.idap.common.model.ResultGenerator;
import org.junit.jupiter.api.Test;

class ResultGeneratorTest {

    @Test
    void generatesSuccessResultWithData() {
        Result<String> result = ResultGenerator.genSuccessResult("ok");

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getMessage()).isEqualTo("SUCCESS");
        assertThat(result.getData()).isEqualTo("ok");
    }

    @Test
    void generatesFailureResultFromException() {
        Result result = ResultGenerator.genFailResult(new IllegalArgumentException("bad request"));

        assertThat(result.getCode()).isEqualTo(400);
        assertThat(result.getMessage()).isEqualTo("bad request");
    }
}
