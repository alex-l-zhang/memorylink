import 'package:shared_preferences/shared_preferences.dart';

class AuthSession {
  final String token;
  final int userId;
  final String phone;

  const AuthSession({required this.token, required this.userId, required this.phone});
}

class SessionStore {
  static const _tokenKey = 'memorylink_token';
  static const _userIdKey = 'memorylink_user_id';
  static const _phoneKey = 'memorylink_phone';

  static Future<AuthSession?> load() async {
    final prefs = await SharedPreferences.getInstance();
    final token = prefs.getString(_tokenKey);
    final userId = prefs.getInt(_userIdKey);
    final phone = prefs.getString(_phoneKey);
    if (token == null || userId == null) return null;
    return AuthSession(token: token, userId: userId, phone: phone ?? '');
  }

  static Future<void> save(AuthSession session) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(_tokenKey, session.token);
    await prefs.setInt(_userIdKey, session.userId);
    await prefs.setString(_phoneKey, session.phone);
  }

  static Future<void> clear() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.remove(_tokenKey);
    await prefs.remove(_userIdKey);
    await prefs.remove(_phoneKey);
  }
}
