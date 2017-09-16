package com.myimooc.seckill.web;

import com.myimooc.seckill.dto.Exposer;
import com.myimooc.seckill.dto.SeckillExecution;
import com.myimooc.seckill.dto.SeckillResult;
import com.myimooc.seckill.entity.Seckill;
import com.myimooc.seckill.enums.SeckillStatEnum;
import com.myimooc.seckill.exception.RepeatKillException;
import com.myimooc.seckill.exception.SeckillCloseException;
import com.myimooc.seckill.service.SeckillService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.util.Date;
import java.util.List;

/**
 * @describe 控制器
 * @author zc
 * @version 1.0 2017-08-24
 */
@Controller
@RequestMapping(value = "/seckill")// url:/模块/资源/{id}/细分
public class SeckillController {

    public SeckillController(){
        System.out.println("SeckillController被初始化");
    }

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private SeckillService seckillService;

    // 该注解为组合注解，等价于 @RequestMapping(name="/list",method=RequestMethod.GET)
    @GetMapping(value = "/list")
    public ModelAndView list(Model model){
        logger.info("进入列表页");
        // 获取列表页
        List<Seckill> list = seckillService.getSeckillList();
        logger.info("list = {}",list);
        model.addAttribute(list);
        // list.jsp + model = ModelAndView
        return new ModelAndView("list").addObject("list",list); //    /WEB-INF/jsp/list.jsp
    }

    @GetMapping("/{seckillId}/detail")
    public String detail(@PathVariable("seckillId") Long seckillId, Model model){
        if(null == seckillId){
            // 重定向
            return "redirect:/seckill/list";
        }
        Seckill seckill = seckillService.getById(seckillId);
        if(null == seckill){
            // 请求转发
            return "forward:/seckill/list";
        }
        model.addAttribute("seckill",seckill);
        return "detail";
    }

    // ajax json
    @PostMapping(value = "/{seckillId}/exposer",produces = {"application/json;charset=UTF-8"})
    @ResponseBody
    public SeckillResult<Exposer> exposer(@PathVariable("seckillId")Long seckillId){
        SeckillResult<Exposer> result;
        try {
            Exposer exposer = seckillService.exportSeckillUrl(seckillId);
            result = new SeckillResult<Exposer>(true,exposer);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            result = new SeckillResult<Exposer>(false, e.getMessage());
        }
        return result;
    }

    @PostMapping(value = "/{seckillId}/{md5}/execution",produces = {"application/json;charset=UTF-8"})
    @ResponseBody
    public SeckillResult<SeckillExecution> execute(@PathVariable("seckillId")Long seckillId,
                                                    @PathVariable("md5")String md5,
                                                    @CookieValue(value = "killPhone",required = false) Long phone){
        if(StringUtils.isEmpty(phone)){
            return new SeckillResult<SeckillExecution>(false,"未注册");
        }
//        SeckillResult<SeckillExecution> result;
        try{
            // 存储过程调用
            SeckillExecution execution = seckillService.executeSeckill(seckillId,phone,md5);

            //SeckillExecution execution = seckillService.executeSeckill(seckillId,phone,md5);
            return new SeckillResult<SeckillExecution>(true,execution);
        } catch (SeckillCloseException e1){
            SeckillExecution execution = new SeckillExecution(seckillId, SeckillStatEnum.END);
            return new SeckillResult<SeckillExecution>(true,execution);
        } catch (RepeatKillException e2){
            SeckillExecution execution = new SeckillExecution(seckillId, SeckillStatEnum.REPEAT_KILL);
            return new SeckillResult<SeckillExecution>(true,execution);
        } catch (Exception e){
            logger.error(e.getMessage(),e);
            SeckillExecution execution = new SeckillExecution(seckillId, SeckillStatEnum.INNER_ERROR);
            return new SeckillResult<SeckillExecution>(true,execution);
        }
    }

    @GetMapping("/time/now")
    @ResponseBody
    public SeckillResult<Long> time(){
        Date now = new Date();
        return new SeckillResult<Long>(true,now.getTime());
    }
}