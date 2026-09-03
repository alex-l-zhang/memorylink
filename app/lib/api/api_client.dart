import 'dart:convert';
import 'dart:typed_data';

import 'package:http/http.dart' as http;
import 'package:http_parser/http_parser.dart';

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

  Future<LovedOne> updateLovedOne(
    String token,
    int id, {
    required String name,
    String? birthDate,
    String? deathDate,
    String? birthPlace,
    String? bio,
  }) async {
    final data = await _put('/api/v1/lovedones/$id', {
      'name': name,
      'birthDate': birthDate == null || birthDate.isEmpty ? null : birthDate,
      'deathDate': deathDate == null || deathDate.isEmpty ? null : deathDate,
      'birthPlace': birthPlace == null || birthPlace.isEmpty ? null : birthPlace,
      'bio': bio == null || bio.isEmpty ? null : bio,
    }, token: token);
    return LovedOne.fromJson(data as Map<String, dynamic>);
  }

  Future<ChatResult> chat(String token, int lovedOneId, String question) async {
    final data = await _post('/api/v1/lovedones/$lovedOneId/chat', {'question': question}, token: token);
    return ChatResult.fromJson(data as Map<String, dynamic>);
  }

  Future<List<MediaItem>> listMedia(String token, int lovedOneId) async {
    final data = await _get('/api/v1/lovedones/$lovedOneId/media', token: token);
    return (data as List)
        .map((e) => MediaItem.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  Future<String> getMediaUrl(String token, int lovedOneId, int mediaId) async {
    final data = await _get('/api/v1/lovedones/$lovedOneId/media/$mediaId/url', token: token);
    return (data as Map<String, dynamic>)['url'] as String;
  }

  Future<MediaItem> uploadMedia(
    String token,
    int lovedOneId,
    String mediaType,
    String filename,
    Uint8List bytes,
  ) async {
    final uri = Uri.parse('$baseUrl/api/v1/lovedones/$lovedOneId/media?mediaType=$mediaType');
    final request = http.MultipartRequest('POST', uri)
      ..headers['Authorization'] = 'Bearer $token'
      ..files.add(http.MultipartFile.fromBytes(
        'file',
        bytes,
        filename: filename,
        contentType: MediaType.parse(_contentTypeFor(filename)),
      ));
    final streamed = await request.send();
    final response = await http.Response.fromStream(streamed);
    final data = _decode(response);
    return MediaItem.fromJson(data as Map<String, dynamic>);
  }

  String _contentTypeFor(String filename) {
    final lower = filename.toLowerCase();
    if (lower.endsWith('.png')) return 'image/png';
    if (lower.endsWith('.jpg') || lower.endsWith('.jpeg')) return 'image/jpeg';
    if (lower.endsWith('.gif')) return 'image/gif';
    if (lower.endsWith('.mp3')) return 'audio/mpeg';
    if (lower.endsWith('.m4a') || lower.endsWith('.aac')) return 'audio/mp4';
    if (lower.endsWith('.wav')) return 'audio/wav';
    if (lower.endsWith('.mp4')) return 'video/mp4';
    return 'application/octet-stream';
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

  Future<dynamic> _put(String path, Map<String, dynamic> body, {String? token}) async {
    final response = await http.put(
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
