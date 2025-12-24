package jame.work.filesserver.entity;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class RecycleResult {
    private List<String> success = new ArrayList<>();
    private List<String> failed = new ArrayList<>();
}