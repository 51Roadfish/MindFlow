export function stripMarkdown(md: string): string {
  if (!md) return '';
  let text = md;

  // 代码块 ```lang ... ```
  text = text.replace(/```[\s\S]*?```/g, '');
  // 行内代码 `code`
  text = text.replace(/`([^`]+)`/g, '$1');

  // 图片 ![alt](url)
  text = text.replace(/!\[([^\]]*)\]\([^)]+\)/g, '$1');
  // 链接 [text](url) — 保留链接文字
  text = text.replace(/\[([^\]]+)\]\([^)]+\)/g, '$1');

  // 标题标记 ## → 纯文本
  text = text.replace(/^#{1,6}\s+/gm, '');
  // 引用
  text = text.replace(/^>\s*/gm, '');

  // 无序列表 - + *
  text = text.replace(/^[-*+]\s+/gm, '');
  // 有序列表
  text = text.replace(/^\d+\.\s+/gm, '');

  // 加粗 **text** __text__
  text = text.replace(/(\*\*|__)(.*?)\1/g, '$2');
  // 斜体 *text_ _text_
  text = text.replace(/(\*|_)(.*?)\1/g, '$2');
  // 删除线 ~~text~~
  text = text.replace(/~~(.*?)~~/g, '$1');

  // 分割线 --- *** ___
  text = text.replace(/^[-*_]{3,}\s*$/gm, '');

  // 压缩多余空行
  text = text.replace(/\n{3,}/g, '\n\n');

  return text.trim();
}
