package com.testerhome.appcrawler.plugin

import com.testerhome.appcrawler.URIElement
import com.testerhome.appcrawler.ElementStatus

/**
  * Created by seveniruby on 16/1/21.
  *
  * 如果某种类型的控件点击次数太多, 就跳过. 设定一个阈值
  */
class TagLimitPlugin extends Plugin {
  private val tagLimit = scala.collection.mutable.Map[String, Int]()
  private var tagLimitMax = 3

  override def start(): Unit = {
    tagLimitMax = getCrawler().conf.tagLimitMax
  }

  override def beforeElementAction(element: URIElement): Unit = {
    val key = element.getAncestor()
    log.trace(s"tag path = ${key}")
    if (!tagLimit.contains(key)) {
      //跳过具备selected=true的菜单栏
      getCrawler().driver.findMapByKey("//*[@selected='true']").foreach(m=>{
        val element=getCrawler().getUrlElementByMap(m)
        tagLimit(element.getAncestor())=20
        log.info(s"tagLimit[${element.getAncestor()}]=20")
      })
      //应用定制化的规则
      getTimesFromTagLimit(element) match {
        case Some(v)=> {
          tagLimit(key)=v
          log.info(s"tagLimit[${key}]=${tagLimit(key)} with conf.tagLimit")
        }
        case None => tagLimit(key)=tagLimitMax
      }
    }

    //如果达到限制次数就退出
    if (key.nonEmpty && tagLimit(key) <= 0) {
      log.warn(s"tagLimit[${key}]=${tagLimit(key)}")
      getCrawler().setElementAction("skip")
      log.info(s"$element need skip")
    }
  }

  override def afterElementAction(element: URIElement): Unit = {
    if(getCrawler().getElementAction()!="clear") {
      val key = element.getAncestor()
      if (tagLimit.contains(key)) {
        tagLimit(key) -= 1
        log.info(s"tagLimit[${key}]=${tagLimit(key)}")
      }
    }
  }


  override def afterUrlRefresh(url: String): Unit = {

  }

  def getTimesFromTagLimit(element: URIElement): Option[Int] = {
    this.getCrawler().conf.tagLimit.foreach(tag => {
      if(getCrawler().driver.findMapByKey(tag.getXPath())
        .map(new URIElement(_, getCrawler().currentUrl))
        .contains(element)){
        return Some(tag.times)
      }else{
        None
      }
    })
    None
  }

}