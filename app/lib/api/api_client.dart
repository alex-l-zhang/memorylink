import 'dart:convert';

import 'package:http/http.dart' as http;

import '../models.dart';

class ApiException implements Exception {
  final int code;
  final String message;

  ApiException(this.code, this.message);

  @override
  String toString() => message;
}

class ApiClient {
  final String baseUrl;

  ApiClient({required this.baseUrl});

  Future<AuthResult> login({required String phone, required String password}) async {
    final data = await _post('/api/v1/auth/login', {'phone': phone, 'password': password});
    return AuthResult.fromJson(data as Map<String, dynamic>);
  }

  Future<AuthResult> register({
    required String phone,
    required String name,
    required String password,
  }) async {
    final data = await _post('/api/v1/auth/register', {
      'phone': phone,
      'name': name,
      'password': password,
    });
    return AuthResult.fromJson(data as Map<String, dynamic>);
  }

  Future<List<LovedOne>> listLovedOnes(String token) async {
    final data = await _get('/api/v1/lovedones', token: token);
    return (data as List)
        .map((e) => LovedOne.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  Future<LovedOne> createLovedOne(String token, String name) async {
    final data = await _post('/api/v1/lovedones', {'name': name}, token: token);
    return LovedOne.fromJson(data as Map<String, dynamic>);
  }

  Future<ChatResult> chat(String token, int lovedOneId, String question) async {
    final data = await _post('/api/v1/lovedones/$lovedOneId/chat', {'question': question}, token: token);
    return ChatResult.fromJson(data as Map<String, dynamic>);
  }

  Map<String, String> _headers(String? token) => {
        'Content-Type': 'application/json',
        if (token != null) 'Authorization': 'Bearer $token',
      };

  Future<dynamic> _get(String path, {String? token}) async {
    final response = await http.get(Uri.parse('$baseUrl$path'), headers: _headers(token));
    return _decode(response);
  }

  Future<dynamic> _post(String path, Map<String, dynamic> body, {String? token}) async {
    final response = await http.post(
      Uri.parse('$baseUrl$path'),
      headers: _headers(token),
      body: jsonEncode(body),
    );
    return _decode(response);
  }

  dynamic _decode(http.Response response) {
    final map = jsonDecode(utf8.decode(response.bodyBytes)) as Map<String, dynamic>;
    final code = map['code'] as int? ?? 5000;
    if (code != 0) {
      throw ApiException(code, map['message'] as String? ?? '请求失败，请稍后重试');
    }
    return map['data'];
  }
}
