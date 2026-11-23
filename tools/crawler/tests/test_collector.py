import unittest
from collector import extract_candidates, robots_allowed

class CollectorTest(unittest.TestCase):
    def test_extracts_only_configured_candidates_without_full_article(self):
        html='<html><head><title>旅行指南</title></head><body><script>南泥湾革命旧址</script><p>杨家岭革命旧址值得参观。</p></body></html>'
        result=extract_candidates(html,['杨家岭革命旧址','南泥湾革命旧址'])
        self.assertEqual(result['title'],'旅行指南')
        self.assertEqual(result['candidates'],['杨家岭革命旧址'])
        self.assertNotIn('body',result)
    def test_robots_rejects_disallowed_paths(self):
        robots='User-agent: *\nDisallow: /a/\nAllow: /travel/\n'
        self.assertFalse(robots_allowed(robots,'https://example.com/a/123'))
        self.assertTrue(robots_allowed(robots,'https://example.com/travel/123'))
if __name__=='__main__': unittest.main()
