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
  RelationOption('FRIEND', '朋友'),
  RelationOption('OTHER', '其他'),
];

// 主动建立联系时："我是 TA 的……"（发起人视角）
const List<RelationOption> connectionRelationOptions = [
  RelationOption('SPOUSE', '配偶'),
  RelationOption('PARENT', 'TA 的父母'),
  RelationOption('CHILD', 'TA 的子女'),
  RelationOption('GRANDPARENT', 'TA 的祖辈'),
  RelationOption('GRANDCHILD', 'TA 的孙辈'),
  RelationOption('SIBLING', '兄弟姐妹'),
  RelationOption('FRIEND', '朋友'),
  RelationOption('OTHER', '其他'),
];
