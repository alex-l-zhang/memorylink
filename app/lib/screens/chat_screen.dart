import 'package:flutter/material.dart';

import '../api/api_client.dart';
import '../models.dart';

class ChatScreen extends StatefulWidget {
  final ApiClient api;
  final String token;
  final LovedOne lovedOne;

  const ChatScreen({super.key, required this.api, required this.token, required this.lovedOne});

  @override
  State<ChatScreen> createState() => _ChatScreenState();
}

class _ChatScreenState extends State<ChatScreen> {
  final _input = TextEditingController();
  final List<_Message> _messages = [];
  bool _sending = false;

  @override
  void dispose() {
    _input.dispose();
    super.dispose();
  }

  Future<void> _send() async {
    final question = _input.text.trim();
    if (question.isEmpty || _sending) return;
    _input.clear();
    setState(() {
      _sending = true;
      _messages.add(_Message(text: question, fromUser: true, ai: false));
    });
    try {
      final result = await widget.api.chat(widget.token, widget.lovedOne.id, question);
      if (!mounted) return;
      setState(() => _messages.add(_Message(text: result.answer, fromUser: false, ai: result.aiFlag)));
      if (result.usageHint != null) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(result.usageHint!)));
      }
    } on ApiException catch (e) {
      if (mounted) {
        setState(() => _messages.add(_Message(text: '出错了：${e.message}', fromUser: false, ai: false)));
      }
    } catch (_) {
      if (mounted) {
        setState(() => _messages.add(const _Message(text: '网络异常，请稍后重试', fromUser: false, ai: false)));
      }
    } finally {
      if (mounted) setState(() => _sending = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text('和 ${widget.lovedOne.name} 聊聊天')),
      body: Column(
        children: [
          const Padding(
            padding: EdgeInsets.all(8),
            child: Text('AI 生成内容，仅供情感慰藉参考', style: TextStyle(fontSize: 12, color: Colors.grey)),
          ),
          Expanded(
            child: _messages.isEmpty
                ? const Center(child: Text('问问 TA 的故事吧，例如："小时候最喜欢做什么？"'))
                : ListView.builder(
                    padding: const EdgeInsets.all(12),
                    itemCount: _messages.length,
                    itemBuilder: (context, index) => _MessageBubble(message: _messages[index]),
                  ),
          ),
          SafeArea(
            child: Padding(
              padding: const EdgeInsets.all(8),
              child: Row(
                children: [
                  Expanded(
                    child: TextField(
                      controller: _input,
                      decoration: const InputDecoration(
                        hintText: '输入你的问题…',
                        border: OutlineInputBorder(),
                        isDense: true,
                      ),
                      onSubmitted: (_) => _send(),
                    ),
                  ),
                  const SizedBox(width: 8),
                  IconButton.filled(onPressed: _sending ? null : _send, icon: const Icon(Icons.send)),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _Message {
  final String text;
  final bool fromUser;
  final bool ai;

  const _Message({required this.text, required this.fromUser, required this.ai});
}

class _MessageBubble extends StatelessWidget {
  final _Message message;

  const _MessageBubble({required this.message});

  @override
  Widget build(BuildContext context) {
    return Align(
      alignment: message.fromUser ? Alignment.centerRight : Alignment.centerLeft,
      child: Container(
        margin: const EdgeInsets.symmetric(vertical: 4),
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
        constraints: const BoxConstraints(maxWidth: 320),
        decoration: BoxDecoration(
          color: message.fromUser
              ? Theme.of(context).colorScheme.primaryContainer
              : Theme.of(context).colorScheme.surfaceContainerHighest,
          borderRadius: BorderRadius.circular(12),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(message.text),
            if (!message.fromUser && message.ai)
              const Padding(
                padding: EdgeInsets.only(top: 4),
                child: Text('AI 生成', style: TextStyle(fontSize: 10, color: Colors.grey)),
              ),
          ],
        ),
      ),
    );
  }
}
