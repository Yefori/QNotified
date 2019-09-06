package nil.nadph.qnotified.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Handler;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.text.util.Linkify;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import nil.nadph.qnotified.record.ConfigManager;

import javax.net.ssl.HttpsURLConnection;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static nil.nadph.qnotified.util.Utils.log;

public class UpdateCheck implements View.OnClickListener, Runnable {

    private ViewGroup viewGroup;
    private boolean clicked = false;
    private PHPArray result;
    public static final String UPDATE_INFO = "https://raw.githubusercontent.com/cinit/QNotified/master/update_info";
    private int runlevel;
    public static final String qn_update_info = "qn_update_info";
    public static final String qn_update_time = "qn_update_time";
    int currVerCode = Utils.QN_VERSION_CODE;
    String currVerName = Utils.QN_VERSION_NAME;
    private final int RL_LOAD = 1;
    private final int RL_SHOW_RET = 2;

    public UpdateCheck() {
    }

    public String doRefreshInfo() {
        String content = null;
        try {
            URL reqURL = new URL(UPDATE_INFO);
            HttpsURLConnection httpsConn = (HttpsURLConnection) reqURL.openConnection();
            InputStream in = httpsConn.getInputStream();
            ByteArrayOutputStream bais = new ByteArrayOutputStream();
            byte[] buf = new byte[256];
            int len;
            while ((len = in.read(buf)) != -1) {
                bais.write(buf, 0, len);
            }
            in.close();
            content = bais.toString("UTF-8");
            httpsConn.disconnect();
            ConfigManager cfg = ConfigManager.getDefault();
            cfg.putString(qn_update_info, content);
            cfg.getAllConfig().put(qn_update_time, System.currentTimeMillis() / 1000L);
            cfg.save();
        } catch (IOException e) {
            runlevel = 0;
            if (content == null)
                new Handler(viewGroup.getContext().getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(viewGroup.getContext(), "检查更新失败:" + e, Toast.LENGTH_SHORT).show();
                    }
                });
        }
        return content;
    }

    private String getCachedUpdateInfoOrNull() {
        try {
            ConfigManager cfg = ConfigManager.getDefault();
            String str = cfg.getString(qn_update_info);
            long time = cfg.getLongOrDefault(qn_update_time, 0);
            if (System.currentTimeMillis() / 1000L - time > 3 * 24 * 3600) return null;
            return str;
        } catch (Exception e) {
            log(e);
            return null;
        }

    }

    public void setVersionTip(ViewGroup vg) {
        viewGroup = vg;
        try {
            TextView tv_v = viewGroup.findViewById(QQViewBuilder.R_ID_VALUE);
            TextView tv_t = viewGroup.findViewById(QQViewBuilder.R_ID_TITLE);
            String str = getCachedUpdateInfoOrNull();
            if (str != null) {
                String highest = currVerName;
                int hv = currVerCode;
                for (Object obj : PHPArray.fromJson(str)._$_E()) {
                    PHPArray info = (PHPArray) obj;
                    int v = ((Number) info.__("code")._$()).intValue();
                    if (v > hv) {
                        hv = v;
                        highest = info.__("name")._$().toString();
                    }
                }
                if (hv > currVerCode) {
                    //has newer
                    tv_v.setText(highest);
                    tv_v.setTextColor(Color.argb(255, 242, 140, 72));
                    tv_t.setText("有新版本可用");
                    if (clicked) {
                        doShowUPdateInfo();
                    }
                } else {
                    tv_v.setText("已是最新");
                }
            }
        } catch (Exception e) {
            log(e);
        }
    }

    @Override
    public void run() {
        switch (runlevel) {
            case RL_LOAD:
                String ret = doRefreshInfo();
                if (ret == null) return;
                runlevel = 2;
                result = PHPArray.fromJson(ret);
                new Handler(viewGroup.getContext().getMainLooper()).post(this);
                return;
            case RL_SHOW_RET:
                TextView tv_v = viewGroup.findViewById(QQViewBuilder.R_ID_VALUE);
                TextView tv_t = viewGroup.findViewById(QQViewBuilder.R_ID_TITLE);
                String highest = currVerName;
                int hv = currVerCode;
                for (Object obj : result._$_E()) {
                    PHPArray info = (PHPArray) obj;
                    int v = ((Number) info.__("code")._$()).intValue();
                    if (v > hv) {
                        hv = v;
                        highest = info.__("name")._$().toString();
                    }
                }
                if (hv > currVerCode) {
                    //has newer
                    tv_v.setText(highest);
                    tv_v.setTextColor(Color.argb(255, 242, 140, 72));
                    tv_t.setText("有新版本可用");
                    if (clicked) {
                        doShowUPdateInfo();
                    }
                } else {
                    tv_v.setText("已是最新");
                }
                runlevel = 0;
                if (clicked) doShowUPdateInfo();
                return;
        }
    }

    private Class cl_QQCustomDialog;

    private void doShowUPdateInfo() {
        clicked = false;
        Activity ctx = (Activity) viewGroup.getContext();
        AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
        builder.setCancelable(true);
        builder.setNegativeButton("关闭", null);
        /*PopupWindow pop=new PopupWindow();
        pop.setWidth(WRAP_CONTENT);
        pop.setHeight(WRAP_CONTENT);*/
        LinearLayout main = new LinearLayout(ctx);
        //pop.setContentView(main);
        main.setOrientation(LinearLayout.VERTICAL);
        ScrollView scrollView = new ScrollView(ctx);
        builder.setView(scrollView);
        scrollView.addView(main, WRAP_CONTENT, WRAP_CONTENT);
        SpannableStringBuilder sb = new SpannableStringBuilder();
        //StringBuilder sb=new StringBuilder();
        TextView list = new TextView(ctx);
        main.addView(list, WRAP_CONTENT, WRAP_CONTENT);
        list.setAutoLinkMask(Linkify.WEB_URLS);
        for (Object obj : result._$_E()) {
            PHPArray ver = (PHPArray) obj;
            String vn = (String) ver.__("name")._$();
            String vc = "" + ver.__("code")._$();
            String desc = "" + ver.__("desc")._$();
            String md5 = (String) ver.__("md5")._$();
            long time = ((Number) ver.__("time")._$()).longValue();
            String date = Utils.getRelTimeStrSec(time);
            boolean taichi = ver.__("taichi")._$b();
            boolean beta = ver.__("beta")._$b();
            SpannableString tmp = new SpannableString(vn + " (" + vc + ")");
            tmp.setSpan(new RelativeSizeSpan(1.8f), 0, tmp.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            sb.append(tmp);
            sb.append("发布于" + date);
            sb.append(beta ? " (测试版) " : "");
            sb.append('\n');
            if (taichi) sb.append("已适配太极\n");
            sb.append("md5:" + md5 + "\n");
            sb.append(desc);
            sb.append("\n下载地址:\n");
            for (Object obj2 : ver.__("urls")._$_E()) {
                sb.append(obj2 + "\n");
            }
            sb.append("\n");
        }
        list.setText(sb);
        builder.create().show();
    }

    @Override
    public void onClick(View v) {
        viewGroup = (ViewGroup) v;
        clicked = true;
        if (result == null) {
            runlevel = 1;
            new Thread(this).start();
        } else {
            doShowUPdateInfo();
        }
    }
}