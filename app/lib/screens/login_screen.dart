import 'package:flutter/material.dart';

import '../api/api_client.dart';
import '../auth/session_store.dart';
import 'home_screen.dart';

class LoginScreen extends StatefulWidget {
  final ApiClient api;

  const LoginScreen({super.key, required this.api});

  @override
  State<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends State<LoginScreen> {
  final _phone = TextEditingController();
  final _password = TextEditingController();
  final _confirmPassword = TextEditingController();
  final _name = TextEditingController();
  bool _registerMode = false;
  bool _loading = false;
  String? _error;

  @override
  void dispose() {
    _phone.dispose();
    _password.dispose();
    _confirmPassword.dispose();
    _name.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    if (_registerMode && _password.text != _confirmPassword.text) {
      setState(() => _error = '两次输入的密码不一致');
      return;
    }
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final result = _registerMode
          ? await widget.api.register(
              phone: _phone.text.trim(),
              name: _name.text.trim(),
              password: _password.text,
              confirmPassword: _confirmPassword.text,
            )
          : await widget.api.login(phone: _phone.text.trim(), password: _password.text);
      await SessionStore.save(AuthSession(
        token: result.token,
        userId: result.userId,
        phone: result.phone,
      ));
      if (!mounted) return;
      Navigator.of(context).pushReplacement(
        MaterialPageRoute(
          builder: (_) => HomeScreen(
            api: widget.api,
            token: result.token,
            userId: result.userId,
          ),
        ),
      );
    } on ApiException catch (e) {
      setState(() => _error = e.message);
    } catch (_) {
      setState(() => _error = '网络异常，请确认后端服务已启动');
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('忆联')),
      body: Center(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(24),
          child: ConstrainedBox(
            constraints: const BoxConstraints(maxWidth: 420),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                Text(_registerMode ? '注册新账号' : '登录',
                    style: Theme.of(context).textTheme.headlineSmall),
                const SizedBox(height: 24),
                if (_registerMode) ...[
                  TextField(
                    controller: _name,
                    decoration: const InputDecoration(labelText: '姓名', border: OutlineInputBorder()),
                  ),
                  const SizedBox(height: 12),
                ],
                TextField(
                  controller: _phone,
                  keyboardType: TextInputType.phone,
                  decoration: const InputDecoration(labelText: '手机号', border: OutlineInputBorder()),
                ),
                const SizedBox(height: 12),
                TextField(
                  controller: _password,
                  obscureText: true,
                  decoration: const InputDecoration(labelText: '密码', border: OutlineInputBorder()),
                ),
                if (_registerMode) ...[
                  const SizedBox(height: 12),
                  TextField(
                    controller: _confirmPassword,
                    obscureText: true,
                    decoration: const InputDecoration(labelText: '确认密码', border: OutlineInputBorder()),
                  ),
                ],
                if (_error != null) ...[
                  const SizedBox(height: 12),
                  Text(_error!, style: TextStyle(color: Theme.of(context).colorScheme.error)),
                ],
                const SizedBox(height: 24),
                FilledButton(
                  onPressed: _loading ? null : _submit,
                  child: Text(_loading ? '请稍候…' : (_registerMode ? '注册并登录' : '登录')),
                ),
                TextButton(
                  onPressed: () => setState(() {
                    _registerMode = !_registerMode;
                    _error = null;
                  }),
                  child: Text(_registerMode ? '已有账号？去登录' : '没有账号？去注册'),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
