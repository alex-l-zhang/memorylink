import 'package:flutter/material.dart';

import '../api/api_client.dart';
import '../models.dart';
import '../relation_options.dart';

class ConnectionsScreen extends StatelessWidget {
  final ApiClient api;
  final String token;

  const ConnectionsScreen({super.key, required this.api, required this.token});

  @override
  Widget build(BuildContext context) {
    return DefaultTabController(
      length: 2,
      child: Scaffold(
        appBar: AppBar(
          title: const Text('建立联系'),
          bottom: const TabBar(tabs: [
            Tab(text: '找人发起'),
            Tab(text: '收到的请求'),
          ]),
        ),
        body: TabBarView(
          children: [
            _SearchTab(api: api, token: token),
            _IncomingTab(api: api, token: token),
          ],
        ),
      ),
    );
  }
}

String _birthText(int? year, int? month) =>
    year == null || month == null ? '出生信息未填写' : '$year 年 $month 月';

String _relationLabel(String code) => connectionRelationOptions
        .where((o) => o.code == code)
        .map((o) => o.label)
        .firstOrNull ??
    code;

class _SearchTab extends StatefulWidget {
  final ApiClient api;
  final String token;

  const _SearchTab({required this.api, required this.token});

  @override
  State<_SearchTab> createState() => _SearchTabState();
}

class _SearchTabState extends State<_SearchTab> {
  final _name = TextEditingController();
  List<UserCandidate> _results = [];
  final Set<int> _selected = {};
  String _relation = 'FRIEND';
  String _inverseRelation = 'FRIEND';
  bool _searching = false;
  bool _sending = false;
  String? _error;
  String? _message;

  @override
  void dispose() {
    _name.dispose();
    super.dispose();
  }

  Future<void> _search() async {
    setState(() {
      _searching = true;
      _error = null;
      _message = null;
    });
    try {
      final results = await widget.api.searchConnections(widget.token, _name.text.trim());
      if (!mounted) return;
      setState(() {
        _results = results;
        _selected.clear();
      });
    } on ApiException catch (e) {
      if (mounted) setState(() => _error = e.message);
    } catch (_) {
      if (mounted) setState(() => _error = '搜索失败，请稍后重试');
    } finally {
      if (mounted) setState(() => _searching = false);
    }
  }

  Future<void> _send() async {
    if (_selected.isEmpty) {
      setState(() => _error = '请先勾选要建立联系的人');
      return;
    }
    setState(() {
      _sending = true;
      _error = null;
      _message = null;
    });
    try {
      await widget.api.sendConnections(
        widget.token,
        targetIds: _selected.toList(),
        relation: _relation,
        inverseRelation: _inverseRelation,
      );
      if (!mounted) return;
      setState(() {
        _message = '已向 ${_selected.length} 人发送联系请求，等待对方同意';
        _selected.clear();
      });
    } on ApiException catch (e) {
      if (mounted) setState(() => _error = e.message);
    } catch (_) {
      if (mounted) setState(() => _error = '发送失败，请稍后重试');
    } finally {
      if (mounted) setState(() => _sending = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        TextField(
          controller: _name,
          decoration: const InputDecoration(
            labelText: '输入完整姓名（需完全匹配）',
            border: OutlineInputBorder(),
          ),
          onSubmitted: (_) => _search(),
        ),
        const SizedBox(height: 8),
        FilledButton.icon(
          onPressed: _searching ? null : _search,
          icon: const Icon(Icons.search),
          label: Text(_searching ? '搜索中…' : '搜索'),
        ),
        if (_error != null)
          Padding(
            padding: const EdgeInsets.only(top: 8),
            child: Text(_error!, style: TextStyle(color: Theme.of(context).colorScheme.error)),
          ),
        if (_message != null)
          Padding(
            padding: const EdgeInsets.only(top: 8),
            child: Text(_message!, style: const TextStyle(color: Colors.green)),
          ),
        const SizedBox(height: 12),
        if (_results.isEmpty && !_searching)
          const Text('搜索结果会显示在这里：姓名、出生年月（不含日）、籍贯。')
        else
          ..._results.map((c) => CheckboxListTile(
                dense: true,
                value: _selected.contains(c.id),
                title: Text(c.name),
                subtitle: Text('${_birthText(c.birthYear, c.birthMonth)}\n籍贯：${c.birthPlace ?? '未填写'}'),
                onChanged: (checked) {
                  setState(() {
                    if (checked == true) {
                      _selected.add(c.id);
                    } else {
                      _selected.remove(c.id);
                    }
                  });
                },
              )),
        if (_results.isNotEmpty) ...[
          const SizedBox(height: 12),
          DropdownButtonFormField<String>(
            initialValue: _relation,
            decoration: const InputDecoration(labelText: 'TA 是我的：', border: OutlineInputBorder()),
            items: connectionRelationOptions
                .map((o) => DropdownMenuItem(value: o.code, child: Text(o.label)))
                .toList(),
            onChanged: (value) {
              if (value != null) {
                setState(() {
                  _relation = value;
                  _inverseRelation = suggestedInverse(value) ?? _inverseRelation;
                });
              }
            },
          ),
          const SizedBox(height: 12),
          DropdownButtonFormField<String>(
            initialValue: _inverseRelation,
            decoration: const InputDecoration(labelText: '我是 TA 的：', border: OutlineInputBorder()),
            items: connectionRelationOptions
                .map((o) => DropdownMenuItem(value: o.code, child: Text(o.label)))
                .toList(),
            onChanged: (value) {
              if (value != null) {
                setState(() {
                  _inverseRelation = value;
                  _relation = suggestedInverse(value) ?? _relation;
                });
              }
            },
          ),
          const SizedBox(height: 12),
          Text(
            '将发送：TA 是我的${_relationLabel(_relation)} · 我是 TA 的${_relationLabel(_inverseRelation)}',
            style: const TextStyle(fontSize: 12, color: Colors.grey),
          ),
          const SizedBox(height: 12),
          FilledButton.icon(
            onPressed: _sending ? null : _send,
            icon: const Icon(Icons.send_outlined),
            label: Text(_sending ? '发送中…' : '发起建立联系'),
          ),
        ],
      ],
    );
  }
}

class _IncomingTab extends StatefulWidget {
  final ApiClient api;
  final String token;

  const _IncomingTab({required this.api, required this.token});

  @override
  State<_IncomingTab> createState() => _IncomingTabState();
}

class _IncomingTabState extends State<_IncomingTab> {
  List<ConnectionRequestItem> _requests = [];
  bool _loading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    _reload();
  }

  Future<void> _reload() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final requests = await widget.api.incomingConnections(widget.token);
      if (!mounted) return;
      setState(() => _requests = requests);
    } on ApiException catch (e) {
      if (mounted) setState(() => _error = e.message);
    } catch (_) {
      if (mounted) setState(() => _error = '加载失败，请稍后重试');
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _respond(ConnectionRequestItem item, bool accept) async {
    try {
      if (accept) {
        await widget.api.acceptConnection(widget.token, item.id);
      } else {
        await widget.api.rejectConnection(widget.token, item.id);
      }
      await _reload();
    } on ApiException catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(e.message)));
      }
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('操作失败，请稍后重试')));
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    if (_loading) return const Center(child: CircularProgressIndicator());
    if (_error != null) {
      return Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Text(_error!),
            const SizedBox(height: 12),
            FilledButton(onPressed: _reload, child: const Text('重试')),
          ],
        ),
      );
    }
    if (_requests.isEmpty) {
      return const Center(child: Text('暂无收到的联系请求'));
    }
    return RefreshIndicator(
      onRefresh: _reload,
      child: ListView.builder(
        padding: const EdgeInsets.all(12),
        itemCount: _requests.length,
        itemBuilder: (context, index) {
          final item = _requests[index];
          return Card(
            child: Padding(
              padding: const EdgeInsets.all(12),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text('${item.requesterName} 想与你建立联系',
                      style: const TextStyle(fontWeight: FontWeight.bold)),
                  const SizedBox(height: 4),
                  Text('对方：${_birthText(item.birthYear, item.birthMonth)} · 籍贯：${item.birthPlace ?? '未填写'}'),
                  Text('对方是我的${_relationLabel(item.inverseRelation ?? item.relation)}'),
                  const SizedBox(height: 8),
                  Row(
                    mainAxisAlignment: MainAxisAlignment.end,
                    children: [
                      OutlinedButton(
                        onPressed: () => _respond(item, false),
                        child: const Text('拒绝'),
                      ),
                      const SizedBox(width: 8),
                      FilledButton(
                        onPressed: () => _respond(item, true),
                        child: const Text('同意'),
                      ),
                    ],
                  ),
                ],
              ),
            ),
          );
        },
      ),
    );
  }
}
