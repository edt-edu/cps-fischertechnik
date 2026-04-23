package io.github.mbdo.factoryscada.domains.dynamicmission.dtos;

import lombok.Data;

import java.util.List;

@Data
public class DynamicMissionDTO {
  String name;
  String description;
  List<String> involvedMachineNames;
}
