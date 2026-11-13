const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const test = require('node:test');
const vm = require('node:vm');

const source = fs.readFileSync(path.resolve(__dirname,
    '../../../main/resources/static/js/admin/pdf-export-failures.js'), 'utf8');

function createElement() {
    return {
        children: [], listeners: {}, style: {}, dataset: {}, textContent: '',
        appendChild(child) { this.children.push(child); },
        append(...children) { this.children.push(...children); },
        replaceChildren() { this.children = []; },
        setAttribute() {},
        addEventListener(event, listener) { this.listeners[event] = listener; },
        showModal() { this.open = true; }
    };
}

async function openDetail(rawEvent) {
    const elements = new Map();
    let onReady;
    let copiedText;
    const document = {
        getElementById(id) {
            if (!elements.has(id)) elements.set(id, createElement());
            return elements.get(id);
        },
        createElement,
        createElementNS: createElement,
        addEventListener(event, listener) { onReady = listener; }
    };
    vm.runInNewContext(source, {
        document,
        navigator: {clipboard: {async writeText(text) { copiedText = text; }}},
        API: {async get(url) {
            return url === '/pdf/failures'
                ? {failures: [], currentPage: 1, totalPages: 0, totalCount: 0}
                : {id: 1, rawEvent};
        }},
        PaginationUtil: {render() {}},
        Auth: {logout() {}}
    });
    onReady();
    await new Promise(setImmediate);
    elements.get('failuresTableBody').listeners.click({
        target: {closest() { return {dataset: {id: '1'}}; }}
    });
    await new Promise(setImmediate);
    assert.equal(elements.get('detailContent').hidden, false);
    const block = elements.get('detailContent').children.find(group =>
        group.children[0].textContent === '원본 실패 이벤트');
    return {
        displayed: block.children[1].children[0].textContent,
        async copy() {
            await block.children[0].children[0].listeners.click();
            return copiedText;
        }
    };
}

test('원본 이벤트의 큰 정수와 소수 및 지수 표기를 유지하며 들여쓴다', async () => {
    const raw = ' { "id":9007199254740993,"decimal":0.123456789012345678901,"exponent":1e+400 } ';
    const detail = await openDetail(raw);
    assert.equal(detail.displayed, [
        '{',
        '  "id": 9007199254740993,',
        '  "decimal": 0.123456789012345678901,',
        '  "exponent": 1e+400',
        '}'
    ].join('\n'));
});

test('복사는 표시용 들여쓰기 대신 공백을 포함한 원문을 그대로 사용한다', async () => {
    const raw = ' \n{"id":9007199254740993, "value":1.2300e+04}\t';
    const detail = await openDetail(raw);
    assert.equal(await detail.copy(), raw);
});

test('문자열 내부의 구분자와 이스케이프 및 중첩 배열과 빈 객체를 보존한다', async () => {
    const raw = String.raw`{"message":"a \"quoted\" \\path { }, : [ ] \n","empty":{},"items":[[],{"value":1}]}`;
    const detail = await openDetail(raw);
    assert.ok(detail.displayed.includes(String.raw`"message": "a \"quoted\" \\path { }, : [ ] \n"`));
    assert.ok(detail.displayed.includes('"empty": {}'));
    assert.ok(detail.displayed.includes('"items": [\n    [],\n    {\n      "value": 1\n    }\n  ]'));
    assert.equal(await detail.copy(), raw);
});
