package com.fordays.masssending.message.website.biz;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.dom4j.Document;
import org.dom4j.Element;
import org.htmlparser.NodeFilter;
import org.htmlparser.Parser;
import org.htmlparser.filters.NodeClassFilter;
import org.htmlparser.tags.FormTag;
import org.htmlparser.tags.InputTag;
import org.htmlparser.tags.SelectTag;
import org.htmlparser.util.NodeList;
import com.fordays.masssending.httpclient.HttpClientInfo;
import com.fordays.masssending.httpclient.HttpClientUtil;
import com.fordays.masssending.message.MessageInfo;
import com.fordays.masssending.message.MessageUtil;
import com.fordays.masssending.message.biz.MessageUtilBiz;
import com.neza.encrypt.XmlUtil;
import com.neza.exception.AppException;

/**
 * 百度贴吧 http://tieba.baidu.com/ 发帖接口实现类
 * 
 * 登录已实现，发帖需要验证码
 */
public class BaiduMessageBizImp extends MessageUtil implements MessageUtilBiz {

	/**
	 * 论坛发帖
	 */
	public MessageInfo publishMessage(MessageInfo messageInfo)
			throws AppException {
		try {
			// 初始化HttpClientInfo
			HttpClientInfo clientInfo = MessageUtil
					.initHttpClientInfo(messageInfo);

			if (clientInfo.isSuccess()) {
				clientInfo = loginWebSite(clientInfo);
				if (clientInfo.isSuccess()) {
					// clientInfo = redirectPostHtml(clientInfo, messageInfo);
					if (clientInfo.isSuccess()) {
						// messageInfo = publishMessage(clientInfo,
						// messageInfo);
						// return messageInfo;
					}
				}
			}
			// messageInfo = setErrorInfo(clientInfo, messageInfo);
			messageInfo = null;
		} catch (Exception e) {
			e.printStackTrace();
			messageInfo.setValidated(false);
			messageInfo.setRemark("不可预期的异常");
		}
		return messageInfo;
	}

	private static Map<String, String> setLoginHiddenInputValue(
			String htmlcontent, Map<String, String> hiddenValue) {
		try {
			Parser parser = Parser.createParser(htmlcontent, "gbk");
			NodeFilter inputTagfilter = new NodeClassFilter(InputTag.class);

			NodeList nodeList = parser.extractAllNodesThatMatch(inputTagfilter);

			String nodeHtml = nodeList.toHtml();
			System.out.println("--nodeHtml--:" + "\n" + nodeHtml);

			// nodeHtml = nodeHtml.replaceAll("<INPUT " + "(.*?)" + ">",
			// "");// 去除多余的标签
			nodeHtml = nodeHtml.replaceAll("disabled", " ");
			nodeHtml = nodeHtml.replaceAll("\"" + ">", "\" />");// 加上结束符，以符合xml规范
			nodeHtml = nodeHtml.replaceAll("=\" \"", "");// 去除多余符号

			String inputHtml = "<form>" + nodeHtml + "</form>";

			XmlUtil xmlutil = new XmlUtil();
			Document doc = xmlutil.readResult(new StringBuffer(inputHtml));
			List nodes = doc.selectNodes("//form/input");
			Iterator it = nodes.iterator();
			while (it.hasNext()) {
				Element elm = (Element) it.next();
				if (elm.attribute("type") != null) {
					String typeValue = elm.attribute("type").getValue();
					if ("button".equals(typeValue)
							|| "checkbox".equals(typeValue)
							|| "checkbox".equals(typeValue)) {
						continue;
					}

					if (elm.attribute("name") != null) {
						String nameValue = elm.attribute("name").getValue();
						if ("token".equals(nameValue)) {
							String value = elm.attribute("value").getValue();
							hiddenValue.put("token", value);
						}

						if ("time".equals(nameValue)) {
							String value = elm.attribute("value").getValue();
							hiddenValue.put("time", value);
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return hiddenValue;
	}

	/**
	 * 论坛发帖
	 * 
	 * @param HttpClientInfo
	 *            clientInfo
	 * @param MessageInfo
	 *            messageInfo
	 */
	public static MessageInfo publishMessage(HttpClientInfo clientInfo,
			MessageInfo messageInfo) throws Exception {
		try {
			String postMethodUrl = messageInfo.getNewTopicUrl();
			String titleValue = messageInfo.getTitle();
			String contentValue = messageInfo.getContent();
			Map<String, String> hiddenValue = clientInfo.getHiddenHtmlValue();
			String formhashStr = hiddenValue.get("formhash").toString();
			String typeidStr = hiddenValue.get("typeid").toString();
			String actionValue = hiddenValue.get("action").toString();

			PostMethod post = new PostMethod(actionValue);
			/**
			 * @特别说明：
			 * @1、需设置自己提交参数的charset,匹配目标网站
			 * @2、由于此处postMethodUrl形如 /post.php?fid=8&referer=http%3A//www.gdin.net
			 * @已经URL编码,所以提交之前须 设置application/x-www-form-urlencoded 予以说明
			 */
			post.setRequestHeader("Content-Type",
					"application/x-www-form-urlencoded;charset=GBK");

			System.out.println("request charset:" + post.getRequestCharSet());

			NameValuePair action = new NameValuePair("action", "newthread");
			NameValuePair extra = new NameValuePair("extra", "");
			NameValuePair topicsubmit = new NameValuePair("topicsubmit", "yes");
			NameValuePair fid = new NameValuePair("fid", "");// 子版ID
			NameValuePair formhash = new NameValuePair("formhash", formhashStr);// 页面取
			NameValuePair wysiwyg = new NameValuePair("wysiwyg", "1");
			NameValuePair iconid = new NameValuePair("iconid", "");
			NameValuePair updateswfattach = new NameValuePair(
					"updateswfattach", "0");
			NameValuePair subject = new NameValuePair("subject", titleValue);// 标题
			NameValuePair typeid = new NameValuePair("typeid", typeidStr);// 页面取
			NameValuePair checkbox = new NameValuePair("checkbox", "0");
			NameValuePair message = new NameValuePair("message", contentValue);// 内容
			NameValuePair tags = new NameValuePair("tags", "");
			NameValuePair htmlon = new NameValuePair("htmlon", "0");
			NameValuePair parseurloff = new NameValuePair("parseurloff", "1");
			NameValuePair smileyoff = new NameValuePair("smileyoff", "1");
			NameValuePair bbcodeoff = new NameValuePair("bbcodeoff", "1");
			NameValuePair tagoff = new NameValuePair("tagoff", "1");
			NameValuePair usesig = new NameValuePair("usesig", "1");
			NameValuePair isanonymous = new NameValuePair("isanonymous", "1");
			NameValuePair emailnotify = new NameValuePair("emailnotify", "1");

			post.setRequestBody(new NameValuePair[] { action, extra,
					topicsubmit, fid, formhash, wysiwyg, iconid,
					updateswfattach, subject, typeid, checkbox, message, tags,
					htmlon, parseurloff, smileyoff, bbcodeoff, tagoff, usesig,
					isanonymous, emailnotify });

			HttpClient client = clientInfo.getHttpclient();

			int statusCode = client.executeMethod(post);

			System.out.println("after publishMessage:" + statusCode);
			HttpClientUtil.printResponseHtml(post, true, false);

			if (200 == statusCode) {
				messageInfo.setSendedStatus(new Long(2));
			} else {
				messageInfo.setSendedStatus(new Long(3));
				messageInfo.setRemark("执行发帖后,statusCode:" + statusCode
						+ ",发帖失败");
				// HttpClientUtil.printResponseHtml(post, "gbk", true);
				System.out.println(post.getResponseBodyAsString());
			}
			post.releaseConnection();
		} catch (Exception e) {
			e.printStackTrace();
			messageInfo.setSendedStatus(new Long(3));
			messageInfo.setRemark("发帖报异常");
		}
		return messageInfo;
	}

	/**
	 * 重定向到发帖页面
	 */
	public HttpClientInfo redirectPostHtml(HttpClientInfo clientInfo,
			MessageInfo messageInfo) throws AppException {
		Map<String, String> hiddenValue = clientInfo.getHiddenHtmlValue();

		try {
			GetMethod redirect = new GetMethod(messageInfo.getNewTopicUrl());
			redirect.setRequestHeader("Cookie", clientInfo.getCookies()
					.toString());

			int statusCode = clientInfo.getHttpclient().executeMethod(redirect);

			if (200 == statusCode) {
				String htmlcontent = printResponseHtml(redirect, true, false);
				// action
				hiddenValue = setActionValue(htmlcontent, hiddenValue);
				System.out.println("action value:" + hiddenValue.get("action"));
				// formhash
				hiddenValue = setHiddenInputValue(htmlcontent, hiddenValue);
				System.out.println("formhash:" + hiddenValue.get("formhash"));
				// 话题
				hiddenValue = setTypeId(htmlcontent, hiddenValue, "闲谈");
				System.out.println("typeid:" + hiddenValue.get("typeid"));

				clientInfo.setHiddenHtmlValue(hiddenValue);
				clientInfo.setSuccess(true);
			} else {
				clientInfo.setSuccess(false);
				clientInfo.setClientRemark("重定向到发帖页面失败");
			}
		} catch (Exception e) {
			e.printStackTrace();
			clientInfo.setSuccess(false);
			clientInfo.setClientRemark("抓取发帖页面信息异常");
		}
		return clientInfo;
	}

	private static Map<String, String> setTypeId(String htmlcontent,
			Map<String, String> hiddenValue, String optionText)
			throws AppException {
		Parser parser = Parser.createParser(htmlcontent, "gbk");
		try {
			NodeFilter selectTagfilter = new NodeClassFilter(SelectTag.class);
			NodeList nodeList = parser
					.extractAllNodesThatMatch(selectTagfilter);

			String nodecontent = "<form>" + nodeList.toHtml() + "</form>";

			// System.out.println("select node content:" + "\n" + nodecontent);

			String typeid = HttpClientUtil.getOptionValueByText(nodecontent,
					optionText);
			hiddenValue.put("typeid", typeid);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return hiddenValue;
	}

	private static Map<String, String> setActionValue(String htmlcontent,
			Map<String, String> hiddenValue) throws AppException {
		try {
			Parser parser = Parser.createParser(htmlcontent, "gbk");
			NodeFilter formTagfilter = new NodeClassFilter(FormTag.class);
			NodeList nodeList = parser.extractAllNodesThatMatch(formTagfilter);

			String content = nodeList.toHtml().substring(0, 150);

			// System.out.println(content);

			int beginIndex = content.indexOf("action");
			int endIndex = content.indexOf("yes\"");

			content = content.substring(beginIndex + 8, endIndex + 3);

			hiddenValue.put("action", "/" + content);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return hiddenValue;
	}

	private static Map<String, String> setHiddenInputValue(String htmlcontent,
			Map<String, String> hiddenValue) {
		try {
			Parser parser = Parser.createParser(htmlcontent, "gbk");
			NodeFilter inputTagfilter = new NodeClassFilter(InputTag.class);

			NodeList nodeList = parser.extractAllNodesThatMatch(inputTagfilter);

			String nodeHtml = nodeList.toHtml();
			// System.out.println("--nodeHtml--:" + "\n" + nodeHtml);

			// nodeHtml = nodeHtml.replaceAll("<INPUT " + "(.*?)" + ">",
			// "");// 去除多余的标签
			nodeHtml = nodeHtml.replaceAll("disabled", " ");
			nodeHtml = nodeHtml.replaceAll("\"" + ">", "\" />");// 加上结束符，以符合xml规范
			nodeHtml = nodeHtml.replaceAll("=\" \"", "");// 去除多余符号

			String inputHtml = "<form>" + nodeHtml + "</form>";

			XmlUtil xmlutil = new XmlUtil();
			Document doc = xmlutil.readResult(new StringBuffer(inputHtml));
			List nodes = doc.selectNodes("//form/input");
			Iterator it = nodes.iterator();
			while (it.hasNext()) {
				Element elm = (Element) it.next();
				if (elm.attribute("type") != null) {
					String typeValue = elm.attribute("type").getValue();
					if ("button".equals(typeValue)
							|| "checkbox".equals(typeValue)
							|| "checkbox".equals(typeValue)) {
						continue;
					}

					if (elm.attribute("name") != null) {
						String nameValue = elm.attribute("name").getValue();
						if ("formhash".equals(nameValue)) {
							String value = elm.attribute("value").getValue();
							hiddenValue.put("formhash", value);
						}

						// if ("posttime".equals(nameValue)) {
						// String value = elm.attribute("value")
						// .getValue();
						// hiddenValue.put("posttime", value);
						// }
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return hiddenValue;
	}

	/**
	 * 登录论坛
	 * 
	 * @param HttpClientInfo
	 *            clientInfo
	 */
	public HttpClientInfo loginWebSite(HttpClientInfo clientInfo)
			throws AppException {
		HttpClient client = clientInfo.getHttpclient();
		try {
			// 模拟登录
			PostMethod post = new PostMethod("http://passport.baidu.com/?login");
			post.setRequestHeader("Content-Type", "charset=gbk");
			NameValuePair tpl_ok = new NameValuePair("tpl_ok", "");
			NameValuePair next_target = new NameValuePair("next_target", "");
			NameValuePair tpl = new NameValuePair("tpl", "");
			NameValuePair skip_ok = new NameValuePair("skip_ok", "");
			NameValuePair aid = new NameValuePair("aid", "");
			NameValuePair need_pay = new NameValuePair("need_pay", "");
			NameValuePair need_coin = new NameValuePair("need_coin", "");
			NameValuePair pay_method = new NameValuePair("pay_method", "");
			NameValuePair u = new NameValuePair("u", "./");
			NameValuePair return_method = new NameValuePair("return_method",
					"get");
			NameValuePair more_param = new NameValuePair("more_param", "");
			NameValuePair return_type = new NameValuePair("return_type", "");
			NameValuePair psp_tt = new NameValuePair("psp_tt", "0");
			NameValuePair password = new NameValuePair("password", clientInfo
					.getLOGIN_PASSWORD());
			NameValuePair safeflg = new NameValuePair("safeflg", "");
			NameValuePair username = new NameValuePair("username", clientInfo
					.getLOGIN_NAME());
			post.setRequestBody(new NameValuePair[] { tpl_ok, next_target, tpl,
					skip_ok, aid, need_pay, need_coin, pay_method, u,
					return_method, more_param, return_type, psp_tt, password,
					safeflg, username });
			int statusCode = 0;

			// statusCode = client.executeMethod(post);
			System.out.println("after execute login method:" + statusCode);

			if (200 == statusCode) {
				// 获取并保存登录Cookie
				clientInfo = HttpClientUtil.saveCurrentCookie(clientInfo);
				clientInfo.setSuccess(true);
			} else {
				clientInfo.setSuccess(false);
				clientInfo.setClientRemark("登录失败,username:" + username
						+ "=password:" + password);
				HttpClientUtil.printResponseHtml(post, true, false);
			}
			post.releaseConnection();
		} catch (Exception e) {
			e.printStackTrace();
			clientInfo.setSuccess(false);
			clientInfo.setClientRemark("登录论坛异常");
		}
		return clientInfo;
	}

	public HttpClientInfo getLoginHtmlInfo(HttpClientInfo clientInfo)
			throws AppException {
		return null;
	}

	public MessageInfo replyMessage(MessageInfo messageInfo)
			throws AppException {
		// TODO Auto-generated method stub
		return null;
	}
}
