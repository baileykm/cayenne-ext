# Cayenne 扩展包

对Cayenne进行了扩展, 添加如下特性:

## CayenneUtil 
使用 _CayenneUtil.getContext();_ 可获得 _ObjectContext_ 对象.
> (1) 默认Cayenne 配置文件为 src 根目录下的 _cayenne-project.xml_  , 若需更改, 可使用 _CayenneUtil.setMapFileName();_
> (2) 获得的 _ObjectContext_ 为应用程序级共享, 若需创建独立的 _ObjectContext_, 可使用  _CayenneUtil.getRuntime().getContext();_

## CountHelper 
根据 _SelectQuery_ 获得满足条件的记录数. 
> public static long count(DataContext context, SelectQuery query);

## EntityUtils
将Cayenne实体对象(集合) 封装为普通VO对象 (集合)
因Cayenne实体对象并非普通Javabean对象 , 在使用类似Gson这样的序列化工具时会生成一些冗余的信息. 若实体之间有外键关系, 还会导致序列化时因递归引用而出现死循环. 因此定义此工具, 用于将 Cayenne 实体对象复制为普通的VO对象  
> public static <T> T packVO(CayenneDataObject entity, Class<T> voClass);
> public static <T> List<T> packVOList(List<? extends CayenneDataObject> entityList, Class<T> voClass);

示例:
```java
List<Artist> artists  = context.performQuery(new SelectQuery(Artist.class));		// Artist 为 Cayenne 实体类
List<VO_Artist> voArtists = EntityUtils.packVOList(artists, VO_Artist.class);
```
* 其中 _voClass_ 为VO类, 其结构应与 Cayenne 实体类对应.
* 可在VO类 ___setter___ 方法上使用 ___@IgnoreSerialize___ 以忽略无须转换的属性. 
* 可使用本人封装的[数据库逆向工程工具包](https://github.com/baileykm/re-engineer)自动生成VO类. 
*  若实体间有外键关系, 序列化时需要将关联的其它实体一同序列化, 可向VO类中添加相应的属性 (注意属性名应与 Cayenne 的 getter 方法对应). 例如:
```java
public class Artist_Ext extends Artist {
	private List<Painting> toPaintingArray;
	
	// setters, getters ...   
}
```
```java
public class Painting_Ext extends Painting {
	private Artist toArtist;
	
	// setters, getters ...   
}
```

## MySQL中UTF-8字符集中文排序修正
使用UTF-8字符集时, 数据库中排序结果并非字典序, 可使用类似如下的代码修正中文排序问题.
> query.addOrdering(new Ordering(Artist.NAME_PROPERTY, SortOrder.ASCENDING)<font color='red'>.convertToGBK(true)</font>);