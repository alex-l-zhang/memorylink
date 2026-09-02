import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:memorylink_app/api/api_client.dart';
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
}
