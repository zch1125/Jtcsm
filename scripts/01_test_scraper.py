# -*- coding: utf-8 -*-
"""
测试爬虫：检查豆果美食页面结构
"""
import requests
from bs4 import BeautifulSoup
import re

requests.packages.urllib3.disable_warnings()

HEADERS = {
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 '
                  'Chrome/120.0.0.0 Safari/537.36',
    'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8',
}

def test_homepage():
    """测试首页，找菜谱列表的结构"""
    url = 'https://www.douguo.com'
    r = requests.get(url, timeout=15, verify=False, headers=HEADERS)
    print(f'首页状态: {r.status_code}, 大小: {len(r.text)}')
    
    soup = BeautifulSoup(r.text, 'html.parser')
    
    # 查找所有链接
    links = soup.select('a')
    print(f'找到 {len(links)} 个含 cookbook 的链接')
    for a in links[:10]:
        href = a.get('href', '')
        text = a.get('title') or a.text.strip()
        if text:
            print(f'  [{text[:40]}] -> {href}')
    
    # 尝试找菜谱链接
    cookbook_links = []
    for a in links:
        href = a.get('href', '')
        if '/cookbook/' in href:
            cookbook_links.append(href)
    print(f'\n找到 {len(cookbook_links)} 个菜谱链接')
    for l in cookbook_links[:10]:
        print(f'  {l}')
    
    # 打印部分HTML看结构
    print(f'\nHTML片段 (body前1000字符):')
    body = soup.find('body')
    if body:
        print(body.text[:1000])
    else:
        print(r.text[:1000])

def test_recipe_detail():
    """测试单个菜谱详情页结构（使用已知ID）"""
    detail_url = 'https://www.douguo.com/cookbook/3355392.html'
    print(f'\n访问菜谱详情: {detail_url}')
    r = requests.get(detail_url, timeout=15, verify=False, headers=HEADERS)
    print(f'详情页状态: {r.status_code}, 大小: {len(r.text)}')
    
    soup = BeautifulSoup(r.text, 'html.parser')
    
    # 保存HTML到文件以便分析
    with open('D:\\Project\\Jtcsm\\scripts\\test_detail.html', 'w', encoding='utf-8') as f:
        f.write(r.text)
    print('HTML已保存到 test_detail.html')
    
    # 检测菜谱相关元素
    checks = [
        ('h1', None),
        ('.recipe-title', None),
        ('.recipe-name', None),
        ('.ingredient', None),
        ('.recipe-ingredient', None),
        ('.step', None),
        ('.recipe-step', None),
        ('.recipe-desc', None),
        ('[class*="ingredient"]', None),
        ('[class*="step"]', None),
        ('[class*="material"]', None),
        ('[class*="cailiao"]', None),
        ('[class*="zuofa"]', None),
        ('[class*="buzhou"]', None),
        ('.cover', None),
    ]
    
    print('\n检查常用选择器:')
    for selector, _ in checks:
        els = soup.select(selector)
        if els:
            for el in els[:3]:
                text = el.text.strip()[:80]
                if text:
                    print(f'  {selector}: {text}')
    
    # 查看页面中的关键CSS类
    all_classes = set()
    for el in soup.find_all(class_=True):
        for c in el.get('class', []):
            all_classes.add(c)
    
    print(f'\n页面CSS类 (含recipe/cookbook/material/step):')
    keywords = ['recipe', 'cookbook', 'material', 'step', 'ingredient', 'cailiao', 'zuofa', 'buzhou', 'title', 'desc', 'cover', 'intro', 'content', 'info']
    for c in sorted(all_classes):
        for kw in keywords:
            if kw in c.lower():
                print(f'  .{c}')
                break
    
    # 查看关键的div结构
    print(f'\n页面主要区域:')
    main_divs = soup.select('div.cont, div.main, div.content, div.recipe, div.detail, div.info, div.body')
    for div in main_divs[:5]:
        cls = ' '.join(div.get('class', []))
        txt = div.text.strip()[:100]
        print(f'  div.{cls}: {txt}')
    
    # 尝试找到食材和步骤
    print(f'\n页面完整文本 (前2000字符):')
    print(r.text[:2000])
    
if __name__ == '__main__':
    # test_homepage()
    print('\n' + '='*60)
    test_recipe_detail()
