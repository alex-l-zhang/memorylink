class RelationOption {
  final String code;
  final String label;

  const RelationOption(this.code, this.label);
}

const List<RelationOption> relationOptions = [
  RelationOption('SPOUSE', '配偶'),
  RelationOption('CHILD', '子女'),
  RelationOption('GRANDCHILD', '孙辈'),
  RelationOption('SIBLING', '兄弟姐妹'),
  RelationOption('OTHER', '其他'),
];
