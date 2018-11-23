package com.opennars.applications.crossing;

public class HexagonMappingTest {
    static public void main(String[] args) {

        //System.out.println(HexagonMapping.isInPolygon(-0.5, 0.5, 0, 0, 10, 0, 0, 10));


        HexagonMapping mapping = new HexagonMapping(5, 10);

        for(int y =0;y<10;y++) {
            for(int x=0;x<30;x++) {

                Vec2Int hex = mapping.map(x, y);

                System.out.print("0123456789abcdef".charAt(hex.hashCode() % 16));
            }

            System.out.println("---");
        }
    }
}
