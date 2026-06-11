package com.elearning.backend.service.code;

import java.util.List;

public interface PreparedCodeProgram extends AutoCloseable {
    int run(List<Integer> args);

    @Override
    void close();
}
