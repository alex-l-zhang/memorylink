import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:memorylink_app/api/api_client.dart';
import 'package:memorylink_app/models.dart';
import 'package:memorylink_app/screens/family_graph_screen.dart';
import 'package:memorylink_app/screens/message_center_screen.dart';

class FakeApi extends ApiClient {
  FakeApi() : super(baseUrl: 'http://127.0.0.1:9');

  bool confirmed = false;
  bool rejected = false;
  bool connectionSent = false;

  @override
  Future<RelationGraph> relationGraph(
    String token, {
    bool includePending = false,
    bool includeExtended = false,
  }) async {
    return RelationGraph(
      self: GraphNode(userId: 1, name: '李小河', myStatus: 'ACTIVE', isSelf: true),
      nodes: [
        GraphNode(
          userId: 2,
          name: '李大山',
          birthYear: 1955,
          birthMonth: 3,
          birthPlace: '浙江绍兴',
          relationshipId: 7,
          relationFromMe: 'FATHER',
          relationFromOther: 'SON',
          myStatus: 'ACTIVE',
          otherStatus: 'ACTIVE',
        ),
        if (includePending)
          GraphNode(
            userId: 3,
            name: '李小明',
            relationshipId: 9,
            myStatus: 'PENDING',
            otherStatus: 'ACTIVE',
            pending: true,
          ),
        if (includeExtended)
          GraphNode(
            userId: 4,
            name: '袁蓉',
            myStatus: 'EXTENDED',
            extended: true,
            viaUserId: 2,
            viaUserName: '张耘嫣',
            relationFromVia: 'MOTHER',
          ),
      ],
      pendingCount: 1,
    );
  }

  @override
  Future<NotificationList> messages(String token) async {
    return NotificationList(
      unread: 1,
      items: [
        NotificationItem(
          id: 5,
          type: 'RELATION_CONFIRM',
          relationshipId: 9,
          otherUserId: 3,
          otherName: '李小明',
          title: '家族成员关系确认',
          body: '李小明 已加入家族，请确认对方是你的谁',
          suggestedRelation: 'FATHER',
          status: 'UNREAD',
        ),
      ],
    );
  }

  @override
  Future<void> confirmMessage(String token, int messageId, String relation) async {
    confirmed = true;
  }

  @override
  Future<void> rejectMessage(String token, int messageId) async {
    rejected = true;
  }

  @override
  Future<void> sendConnections(
    String token, {
    required List<int> targetIds,
    required String relation,
    String? inverseRelation,
  }) async {
    connectionSent = true;
  }
}

void main() {
  testWidgets('图谱默认只显示已确认家人，可开关待确认节点', (tester) async {
    final api = FakeApi();
    await tester.pumpWidget(MaterialApp(home: FamilyGraphScreen(api: api, token: 't')));
    await tester.pumpAndSettle();

    expect(find.text('显示待确认节点'), findsOneWidget);
    expect(find.text('李大山'), findsOneWidget);
    expect(find.text('李小明'), findsNothing);

    await tester.tap(find.descendant(
      of: find.widgetWithText(SwitchListTile, '显示待确认节点'),
      matching: find.byType(Switch),
    ));
    await tester.pumpAndSettle();

    expect(find.text('李小明'), findsOneWidget);
    expect(find.textContaining('待确认'), findsWidgets);
  });

  testWidgets('消息中心按建议称谓确认关系', (tester) async {
    final api = FakeApi();
    await tester.pumpWidget(MaterialApp(home: MessageCenterScreen(api: api, token: 't')));
    await tester.pumpAndSettle();

    expect(find.text('家族成员关系确认'), findsOneWidget);
    expect(find.text('对方是我的：'), findsOneWidget);
    expect(find.text('全部确认为家人'), findsOneWidget);

    await tester.tap(find.text('确认关系'));
    await tester.pumpAndSettle();

    expect(api.confirmed, isTrue);
  });

  testWidgets('图谱可展开家人的家人并一键发起建立联系', (tester) async {
    final api = FakeApi();
    await tester.pumpWidget(MaterialApp(home: FamilyGraphScreen(api: api, token: 't')));
    await tester.pumpAndSettle();

    expect(find.text('显示家人的家人'), findsOneWidget);
    expect(find.text('袁蓉'), findsNothing);

    await tester.tap(find.text('显示家人的家人'));
    await tester.pumpAndSettle();

    expect(find.text('袁蓉'), findsOneWidget);
    expect(find.text('张耘嫣的母亲'), findsOneWidget);

    await tester.tap(find.text('袁蓉'));
    await tester.pumpAndSettle();

    expect(find.textContaining('关系路径：张耘嫣的母亲'), findsOneWidget);
    await tester.tap(find.text('发起建立联系'));
    await tester.pumpAndSettle();

    expect(api.connectionSent, isTrue);
    // 发起成功后弹窗应立即关闭，只保留底部提示
    expect(find.text('发起建立联系'), findsNothing);
    expect(find.textContaining('等待对方同意'), findsOneWidget);
  });
}
