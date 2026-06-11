package com.elearning.backend.service.code;

import com.elearning.backend.entity.CodeProblem;

public interface CodeRunner {
    String getLanguage();

    PreparedCodeProgram prepare(CodeProblem problem, String sourceCode, int argCount);
}
