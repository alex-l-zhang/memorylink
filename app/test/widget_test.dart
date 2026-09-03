import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:memorylink_app/api/api_client.dart';
import 'package:memorylink_app/models.dart';
import 'package:memorylink_app/screens/archive_detail_screen.dart';
import 'package:memorylink_app/screens/login_screen.dart';

void main() {
  testWidgets('登录页渲染手机号、密码与登录按钮', (tester) async {
    await tester.pumpWidget(
      MaterialApp(
        home: LoginScreen(api: ApiClient(baseUrl: 'http://127.0.0.1:9')),
      ),
    );

    expect(find.text('登录'), findsWidgets);
    expect(find.text('手机号'), findsOneWidget);
    expect(find.text('密码'), findsOneWidget);
    expect(find.text('没有账号？去注册'), findsOneWidget);
  });

  testWidgets('切换到注册模式出现姓名字段', (tester) async {
    await tester.pumpWidget(
      MaterialApp(
        home: LoginScreen(api: ApiClient(baseUrl: 'http://127.0.0.1:9')),
      ),
    );

    await tester.tap(find.text('没有账号？去注册'));
    await tester.pump();

    expect(find.text('注册新账号'), findsOneWidget);
    expect(find.text('姓名'), findsOneWidget);
    expect(find.text('注册并登录'), findsOneWidget);
  });

  testWidgets('档案详情页展示信息与操作入口', (tester) async {
    final lovedOne = LovedOne(
      id: 1,
      familyId: 1,
      name: '张爷爷',
      birthDate: '1940-01-01',
      birthPlace: '上海',
    );
    await tester.pumpWidget(
      MaterialApp(
        home: ArchiveDetailScreen(
          api: ApiClient(baseUrl: 'http://127.0.0.1:9'),
          token: 'test-token',
          userId: 1,
          lovedOne: lovedOne,
        ),
      ),
    );

    expect(find.text('张爷爷 的档案'), findsOneWidget);
    expect(find.text('去聊天'), findsOneWidget);
    expect(find.text('编辑资料'), findsOneWidget);
    expect(find.text('添加照片'), findsOneWidget);
    expect(find.text('添加录音'), findsOneWidget);

    await tester.tap(find.text('编辑资料'));
    await tester.pump();
    expect(find.text('保存修改'), findsOneWidget);
  });
}
