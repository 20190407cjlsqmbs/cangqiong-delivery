package com.sky.service.impl;

import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.service.WorkspaceService;
import com.sky.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ReportServiceImpl implements ReportService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private WorkspaceService workspaceService;

    /**
     * 统计指定时间区间内的营业额数据
     * @param begin
     * @param end
     * @return
     */
    public TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end) {
        // 当前集合用于存放时间区间内的所有日期
        List<LocalDate> dateList = new ArrayList<>();

        dateList.add(begin);

        while (!begin.equals(end)){
            // 每次循环给begin加一天，然后添加到集合中
            begin = begin.plusDays(1);
            dateList.add(begin);
        }

        List<Double> turnoverList = new ArrayList<>();
        for (LocalDate date : dateList) {
            // 查询每一天的营业额数据, "已完成"状态的订单
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);

            // select sum(amount) from orders where order_time > ? and order_time < ? and order_status = 5 ;
            Map map = new HashMap();
            map.put("begin", beginTime);
            map.put("end", endTime);
            map.put("status", Orders.COMPLETED);
            Double turnover =  orderMapper.sumByMap(map);
            turnover = turnover == null ? 0.0 : turnover;
            turnoverList.add(turnover);
        }


        return TurnoverReportVO
                .builder()
                .dateList(StringUtils.join(dateList, ","))
                .turnoverList(StringUtils.join(turnoverList, ","))
                .build();
    }

    /**
     * 统计指定时间区间内的用户数据
     * @param begin
     * @param end
     * @return
     */
    public UserReportVO getUserStatistics(LocalDate begin, LocalDate end) {
        // 当前集合用于存放时间区间内的所有日期
        List<LocalDate> dateList = new ArrayList<>();

        dateList.add(begin);
        while (!begin.equals(end)){
            // 每次循环给begin加一天，然后添加到集合中
            begin = begin.plusDays(1);
            dateList.add(begin);
        }

        // 用于存放每天新增用户数量的集合         // select count(id) frome user where create_time > ? and create_time < ? ;
        List<Integer> newUserList = new ArrayList<>();
        // 用于存放每天总用户数量的集合          // select count(id) frome user where create_time < ?  ;
        List<Integer> totalUserList = new ArrayList<>();

        for (LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);

            Map map = new HashMap();
            map.put("end", endTime);
            Integer totalUser = userMapper.countByMap(map);

            map.put("begin", beginTime);
            Integer newUser = userMapper.countByMap(map);

            totalUserList.add(totalUser);
            newUserList.add(newUser);
        }


        return UserReportVO
                .builder()
                .dateList(StringUtils.join(dateList,","))
                .newUserList(StringUtils.join(totalUserList,","))
                .totalUserList(StringUtils.join(totalUserList,","))
                .build();
    }

    /**
     * 统计指定时间区间内的订单数据
     * @param begin
     * @param end
     * @return
     */
    public OrderReportVO getorderStatistics(LocalDate begin, LocalDate end) {

        // 当前集合用于存放时间区间内的所有日期
        List<LocalDate> dateList = new ArrayList<>();

        dateList.add(begin);
        while (!begin.equals(end)){
            // 每次循环给begin加一天，然后添加到集合中
            begin = begin.plusDays(1);
            dateList.add(begin);
        }

        List<Integer> orderCountList = new ArrayList<>();
        List<Integer> vaildOrderCountList = new ArrayList<>();

        // 遍历日期集合，查询每天的订单数据和订单总数
        for (LocalDate date : dateList) {
            // 查询每天的订单数据 select count(id) from orders where order_time > ? and order_time < ? ;
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            Integer orderCount = getOrderCount(beginTime, endTime, null);

            // 查询每天的有效订单数 select count(id) from orders where order_time > ? and order_time < ? and status = 5 ;
            Integer vaildOrderCount = getOrderCount(beginTime, endTime, Orders.COMPLETED);

            orderCountList.add(orderCount);
            vaildOrderCountList.add(vaildOrderCount);
        }

        // 计算时间区间内的总订单数和总有效订单数
        Integer totalOrderCount = orderCountList.stream().reduce(Integer::sum).get();
        Integer vaildOrderCount = vaildOrderCountList.stream().reduce(Integer::sum).get();

        // 计算订单完成率
        Double orderCompletionRate = 0.0;
        if(totalOrderCount != 0){
            orderCompletionRate = vaildOrderCount.doubleValue() / totalOrderCount;

        }
        return OrderReportVO.builder()
                .dateList(StringUtils.join(dateList,","))
                .orderCountList(StringUtils.join(orderCountList,","))
                .validOrderCountList(StringUtils.join(vaildOrderCountList,","))
                .totalOrderCount(totalOrderCount)
                .validOrderCount(vaildOrderCount)
                .orderCompletionRate(orderCompletionRate)
                .build();
    }

    private Integer getOrderCount(LocalDateTime begin, LocalDateTime end, Integer status) {
        Map map = new HashMap();
        map.put("begin", begin);
        map.put("end", end);
        map.put("status", status);

        return orderMapper.countByMap(map);

    }

    /**
     * 统计指定时间区间内的销量前十的商品数据
     * @param begin
     * @param end
     * @return
     */
    public SalesTop10ReportVO getSalesTop10(LocalDate begin, LocalDate end) {

        // select od.name, od.sum(number) nb from order_detail od, orders o where od.order_id = o.id and o.status = 5 and o.order_time > ? and o.order_time < ?
        // group by od.name order by nb desc limit 0,10 ;

        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(begin, LocalTime.MAX);
        List<GoodsSalesDTO> salesTop10 = orderMapper.getSalesTop10(beginTime, endTime);
        List<String> names = salesTop10.stream().map(GoodsSalesDTO::getName).collect(Collectors.toList());
        String nameList = StringUtils.join(names, ",");
        List<Integer> numbers = salesTop10.stream().map(GoodsSalesDTO::getNumber).collect(Collectors.toList());
        String numberList = StringUtils.join(numbers, ",");

        return SalesTop10ReportVO
                .builder()
                .nameList(nameList)
                .numberList(numberList)
                .build();
    }

    /**
     * 导出营业数据报表
     * @param response
     */
    public void exportBusinessData(HttpServletResponse response) {
        // 1、 查询数据库，获取需要导出的数据 —— 最近30天的营业数据
        LocalDate dateBegin = LocalDate.now().minusDays(30);
        LocalDate dateEnd = LocalDate.now().minusDays(1);

        BusinessDataVO businessDataVO = workspaceService.getBusinessData(LocalDateTime.of(dateBegin, LocalTime.MIN), LocalDateTime.of(dateEnd, LocalTime.MAX));
        // 2、 通过 poi 将数据写入到Excel文件中
        InputStream in = this.getClass().getClassLoader().getResourceAsStream("template/运营数据报表模板.xlsx");

        try {
            // 基于模板文件创建一个新的excel 文件
            XSSFWorkbook excel = new XSSFWorkbook(in);

            // 获取sheet页
            XSSFSheet sheet = excel.getSheet("Sheet1");

            // 填充数据到excel文件中
            sheet.getRow(1).getCell(1).setCellValue("时间：" + dateBegin + " 至 " + dateEnd);

            // 获取第4行
            XSSFRow row = sheet.getRow(3);
            row.getCell(2).setCellValue(businessDataVO.getTurnover());
            row.getCell(4).setCellValue(businessDataVO.getOrderCompletionRate());
            row.getCell(6).setCellValue(businessDataVO.getNewUsers());

            // 获取第5行
            row = sheet.getRow(4);
            row.getCell(2).setCellValue(businessDataVO.getValidOrderCount());
            row.getCell(4).setCellValue(businessDataVO.getUnitPrice());

            // 填充明细数据
            for (int i = 0; i < 30 ; i++) {
                LocalDate date = dateBegin.plusDays(i);
                // 查询每天的营业数据
                BusinessDataVO businessData = workspaceService.getBusinessData(LocalDateTime.of(date, LocalTime.MIN), LocalDateTime.of(date, LocalTime.MAX));

                row = sheet.getRow(7 + i );
                row.getCell(1).setCellValue(date.toString());
                row.getCell(2).setCellValue(businessData.getTurnover());
                row.getCell(3).setCellValue(businessData.getValidOrderCount());
                row.getCell(4).setCellValue(businessData.getOrderCompletionRate());
                row.getCell(5).setCellValue(businessData.getUnitPrice());
                row.getCell(6).setCellValue(businessData.getNewUsers());

            }


            // 3、 将Excel文件通过输出流写回给前端
            ServletOutputStream out = response.getOutputStream();
            excel.write(out);

            // 关闭资源
            out.close();
            excel.close();


        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}
