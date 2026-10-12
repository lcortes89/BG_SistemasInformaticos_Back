package com.bgsistemas.api.brand;

import java.util.List;

public interface BrandService {

    List<Brand> getAllBrands();

    Brand getBrandById(Long id);
}
