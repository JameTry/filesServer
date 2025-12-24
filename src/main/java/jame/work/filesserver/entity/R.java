package jame.work.filesserver.entity;

import lombok.Data;

/**
 * @author : Jame
 * @date : 2025/12/23 下午 4:14
 */
@Data
public class R {

    private boolean success;
    private String msg;
    private Object data;


    public static R ok(String msg, Object data) {
        R r = new R();
        r.setSuccess(true);
        r.setMsg(msg);
        r.setData(data);
        return r;
    }
    public static R ok(Object data) {
        return ok("操作成功", data);
    }
    public static R ok() {
        return ok("操作成功");
    }

    public static R error(String msg, Object data) {
        R r = new R();
        r.setSuccess(false);
        r.setMsg(msg);
        r.setData(data);
        return r;
    }
    public static R error(String msg) {
        return error(msg, null);
    }
    public static R error() {
        return error("操作失败");
    }


}
