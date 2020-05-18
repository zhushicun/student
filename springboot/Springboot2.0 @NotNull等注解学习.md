# Springboot2.0 @NotNull等注解学习
# 在校验请求参数是否为null的时候
类属性上

```json
@NotNull
private String 
```
在异常捕捉的类上

```json
@RestControllerAdvice
public class CustomExceptionHandler {
    /**
     * 方法参数校验
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseBody
    public String handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        String msg="";
        int code = ResultStatus.MethodArgumentExceptionCode;
        msg=ex.getBindingResult().getFieldError().getDefaultMessage();
        if(StringUtils.isEmpty(msg)){
            msg=ResultStatus.MethodArgumentExceptionMsg;
        }
        ResultModel<Object> rel=new ResultModel<>(code,msg,null);
        return JSON.toJSONString(rel);
    }
  }
```

这样子就实现了对参数的校验，与@NotNull还有多种注解

```json
@Null  被注释的元素必须为null
@NotNull  被注释的元素不能为null
@AssertTrue  被注释的元素必须为true
@AssertFalse  被注释的元素必须为false
@Min(value)  被注释的元素必须是一个数字，其值必须大于等于指定的最小值
@Max(value)  被注释的元素必须是一个数字，其值必须小于等于指定的最大值
@DecimalMin(value)  被注释的元素必须是一个数字，其值必须大于等于指定的最小值
@DecimalMax(value)  被注释的元素必须是一个数字，其值必须小于等于指定的最大值
@Size(max,min)  被注释的元素的大小必须在指定的范围内。
@Digits(integer,fraction)  被注释的元素必须是一个数字，其值必须在可接受的范围内
@Past  被注释的元素必须是一个过去的日期
@Future  被注释的元素必须是一个将来的日期
@Pattern(value) 被注释的元素必须符合指定的正则表达式。
@Email 被注释的元素必须是电子邮件地址
@Length 被注释的字符串的大小必须在指定的范围内
@NotEmpty  被注释的字符串必须非空
@Range  被注释的元素必须在合适的范围内
```

```json
@NotNull(message = "操作类型不能为Null")
@Range(min = 0,max = 1,message = "操作类型超出值范围")
private Integer os;


/**
 * 设备号 Android取imei/oaid iOS取idfa 取不到则随机生成一个16位随机字符串
 */
@NotNull(message = "deviceId不能为Null")
@NotBlank(message = "deviceId不能为空字符串")
private String deviceId;
```
