package com.halil.e_marketcase.ui.fragment

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.halil.e_marketcase.R
import com.halil.e_marketcase.data.CartItem
import com.halil.e_marketcase.data.Product
import com.halil.e_marketcase.databinding.FragmentProductDetailBinding
import com.halil.e_marketcase.other.Resource
import com.halil.e_marketcase.ui.base.BaseFragment
import com.halil.e_marketcase.ui.viewmodel.CartViewModel
import com.halil.e_marketcase.ui.viewmodel.ProductListViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ProductDetailFragment :
    BaseFragment<FragmentProductDetailBinding>(FragmentProductDetailBinding::inflate) {

    private val cartViewModel: CartViewModel by viewModels({ requireActivity() })
    private val viewModel: ProductListViewModel by viewModels({ requireActivity() })
    private val args: ProductDetailFragmentArgs by navArgs()
    private lateinit var currentProduct: Product

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        currentProduct = args.product
        setupToolbar()
        setupUI()
        observeViewModel()
    }

    private fun setupToolbar() {
        (activity as? AppCompatActivity)?.supportActionBar?.apply {
            title = currentProduct.name
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setupUI() {
        Glide.with(binding.productDetailImage.context)
            .load(currentProduct.image)
            .placeholder(R.drawable.place_holder)
            .into(binding.productDetailImage)

        binding.productName.text = currentProduct.name
        binding.productDescription.text = currentProduct.description
        binding.textViewPriceValue.text = "${currentProduct.price}$"
        updateFavIcon(currentProduct.isFavorite)

        binding.favButtonDetail.setOnClickListener {
            viewModel.toggleFavorite(currentProduct)
            startFavAnimation(binding.favButtonDetail)
        }

        binding.addToCartDetail.setOnClickListener {
            showProgressBar(true)
            addToCart(currentProduct)
        }
    }

    private fun observeViewModel() {
        viewModel.allProducts.observe(viewLifecycleOwner) { products ->
            products.find { it.id == currentProduct.id }?.let {
                currentProduct = it
                updateFavIcon(it.isFavorite)
            }
        }

        cartViewModel.addToCartStatus.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> showProgressBar(true)
                is Resource.Success -> {
                    showProgressBar(false)
                    Toast.makeText(requireContext(), "Product added to cart", Toast.LENGTH_SHORT)
                        .show()
                    findNavController().popBackStack()
                }

                is Resource.Error -> {
                    showProgressBar(false)
                    Toast.makeText(requireContext(), resource.message, Toast.LENGTH_SHORT).show()
                }

                is Resource.Completed -> showProgressBar(false)
            }
        }
    }

    private fun addToCart(product: Product) {
        val price = product.price.toDoubleOrNull() ?: 0.0
        val cartItem = CartItem(name = product.name, price = price)
        Log.d("ProductDetailFragment", "Adding to cart: ${cartItem.name}, ${cartItem.price}")
        cartViewModel.addToCart(cartItem)
    }

    private fun updateFavIcon(isFavorite: Boolean) {
        val favIcon = if (isFavorite) {
            R.drawable.baseline_favorite_24
        } else {
            R.drawable.baseline_favorite_border_24
        }
        binding.favButtonDetail.setImageResource(favIcon)
    }

    private fun startFavAnimation(view: View) {
        val scaleUpX = ObjectAnimator.ofFloat(view, "scaleX", 1.2f)
        val scaleUpY = ObjectAnimator.ofFloat(view, "scaleY", 1.2f)
        val scaleDownX = ObjectAnimator.ofFloat(view, "scaleX", 1.0f)
        val scaleDownY = ObjectAnimator.ofFloat(view, "scaleY", 1.0f)

        val alphaIn = ObjectAnimator.ofFloat(view, "alpha", 0.5f)
        val alphaOut = ObjectAnimator.ofFloat(view, "alpha", 1.0f)

        val animatorSet = AnimatorSet()
        animatorSet.playTogether(scaleUpX, scaleUpY, alphaIn)
        animatorSet.duration = 200
        animatorSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                val resetAnimatorSet = AnimatorSet()
                resetAnimatorSet.playTogether(scaleDownX, scaleDownY, alphaOut)
                resetAnimatorSet.duration = 200
                resetAnimatorSet.start()
            }
        })
        animatorSet.start()
    }

    private fun showProgressBar(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }
}