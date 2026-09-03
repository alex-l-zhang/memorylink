import 'package:flutter/material.dart';

import 'api/api_client.dart';
import 'auth/session_store.dart';
import 'screens/login_screen.dart';
import 'screens/home_screen.dart';

// 通过 --dart-define=API_BASE=http://xxx 覆盖；Android 模拟器访问宿主机请用 http://10.0.2.2:8080
const apiBase = String.fromEnvironment('API_BASE', defaultValue: 'http://192.168.32.128:8080');

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  final session = await SessionStore.load();
  runApp(MemoryLinkApp(api: ApiClient(baseUrl: apiBase), session: session));
}

class MemoryLinkApp extends StatelessWidget {
  final ApiClient api;
  final AuthSession? session;

  const MemoryLinkApp({super.key, required this.api, this.session});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: '忆联',
      theme: ThemeData(colorSchemeSeed: const Color(0xFF6D4C41), useMaterial3: true),
      home: session == null
          ? LoginScreen(api: api)
          : HomeScreen(api: api, token: session!.token, userId: session!.userId),
    );
  }
}
