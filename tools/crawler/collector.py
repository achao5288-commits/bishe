"""Collect only page titles and matching place names for manual review.

No site discovery, redirects, article republication or automatic database writes.
"""
import argparse
import json
import time
from datetime import datetime, timezone
from html.parser import HTMLParser
from pathlib import Path
from urllib.error import HTTPError, URLError
from urllib.parse import urlsplit
from urllib.request import Request, build_opener, HTTPRedirectHandler
from urllib.robotparser import RobotFileParser

AGENT = 'RedTourismResearchBot/0.1'
MAX_BYTES = 2_000_000

class NoRedirect(HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        return None

class PageParser(HTMLParser):
    def __init__(self):
        super().__init__(); self.title_parts=[]; self.text_parts=[]; self.in_title=False; self.ignored=0
    def handle_starttag(self, tag, attrs):
        if tag in ('script','style','noscript'): self.ignored += 1
        if tag == 'title': self.in_title=True
    def handle_endtag(self, tag):
        if tag in ('script','style','noscript') and self.ignored: self.ignored-=1
        if tag == 'title': self.in_title=False
    def handle_data(self, data):
        if self.ignored: return
        if self.in_title: self.title_parts.append(data)
        else: self.text_parts.append(data)

def extract_candidates(html, terms):
    parser=PageParser(); parser.feed(html)
    text=' '.join(parser.text_parts)
    return {'title':' '.join(''.join(parser.title_parts).split()), 'candidates':[term for term in terms if term in text]}

def robots_allowed(robots, url):
    parser=RobotFileParser(); parser.parse(robots.splitlines())
    return parser.can_fetch(AGENT, url)

def fetch(opener, url):
    request=Request(url, headers={'User-Agent':AGENT, 'Accept':'text/html,text/plain;q=0.9'})
    with opener.open(request,timeout=15) as response:
        raw=response.read(MAX_BYTES + 1)
        if len(raw)>MAX_BYTES: raise ValueError('response exceeds size limit')
        return raw.decode(response.headers.get_content_charset() or 'utf-8',errors='replace')

def collect(url, terms):
    parts=urlsplit(url)
    if parts.scheme != 'https' or not parts.hostname or parts.username or parts.password:
        raise ValueError('only HTTPS URLs without credentials are supported')
    opener=build_opener(NoRedirect)
    robots_url=f'{parts.scheme}://{parts.netloc}/robots.txt'
    try:
        robots=fetch(opener,robots_url)
    except (HTTPError, URLError, OSError, ValueError) as exc:
        return {'url':url,'status':'robots_unavailable','detail':str(exc)}
    if not robots_allowed(robots,url): return {'url':url,'status':'robots_disallowed'}
    try:
        parsed=extract_candidates(fetch(opener,url),terms)
    except (HTTPError, URLError, OSError, ValueError) as exc:
        return {'url':url,'status':'fetch_failed','detail':str(exc)}
    return {'url':url,'status':'pending_review','collectedAt':datetime.now(timezone.utc).isoformat(),**parsed}

def main():
    p=argparse.ArgumentParser(description='Respect robots.txt and collect candidate place names for manual review')
    p.add_argument('urls',nargs='+'); p.add_argument('--terms',required=True,help='JSON file containing an array of place names'); p.add_argument('--output',required=True)
    args=p.parse_args(); terms=json.loads(Path(args.terms).read_text(encoding='utf-8-sig'))
    if not isinstance(terms,list) or not all(isinstance(t,str) and t for t in terms): p.error('terms must be a JSON array of nonempty strings')
    results=[]
    for i,url in enumerate(args.urls):
        if i: time.sleep(2)
        results.append(collect(url,terms))
    Path(args.output).write_text(json.dumps(results,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    print(json.dumps(results,ensure_ascii=False,indent=2))
if __name__=='__main__': main()
