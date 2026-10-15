package com.medisense.evaluation.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class EvalSetYaml {

    @JsonProperty("test_cases")
    private List<TestCaseDto> testCases;

    public EvalSetYaml() {}

    public EvalSetYaml(List<TestCaseDto> testCases) {
        this.testCases = testCases;
    }

    public List<TestCaseDto> getTestCases() {
        return testCases;
    }

    public void setTestCases(List<TestCaseDto> testCases) {
        this.testCases = testCases;
    }
}
