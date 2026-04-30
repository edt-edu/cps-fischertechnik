package io.github.mbdo.factoryscada.domains.dynamicmission.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class DynamicMissionDTO {
  String name;
  String description;
  List<String> involvedMachineNames;
}
