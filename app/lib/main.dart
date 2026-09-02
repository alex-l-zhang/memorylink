import 'package:flutter/material.dart';

import 'api/api_client.dart';
import 'screens/login_screen.dart';

// 通过 --dart-define=API_BASE=http://xxx 覆盖；Android 模拟器访问宿主机请用 http://10.0.2.2:8080
const apiBase = String.fromEnvironment('API_BASE', defaultValue: 'http://192.168.32.128:8080');

void main() {
  runApp(MemoryLinkApp(api: ApiClient(baseUrl: apiBase)));
}

class MemoryLinkApp extends StatelessWidget {
  final ApiClient api;

  const MemoryLinkApp({super.key, required this.api});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: '忆联',
      theme: ThemeData(colorSchemeSeed: const Color(0xFF6D4C41), useMaterial3: true),
      home: LoginScreen(api: api),
    );
  }
}
